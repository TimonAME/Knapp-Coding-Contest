package com.knapp.codingcontest.solution;

import java.util.*;

import com.knapp.codingcontest.core.InputDataInternal;
import com.knapp.codingcontest.core.WarehouseInternal;
import com.knapp.codingcontest.data.Bin;
import com.knapp.codingcontest.data.InputData;
import com.knapp.codingcontest.data.Institute;
import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Position;
import com.knapp.codingcontest.data.Shelf;
import com.knapp.codingcontest.operations.CostFactors;
import com.knapp.codingcontest.operations.Warehouse;

public class Solution {
  public String getParticipantName() {
    return "Timon Amesmann";
  }

  public Institute getParticipantInstitution() {
    return Institute.HTL_Rennweg_Wien;
  }

  // ----------------------------------------------------------------------------

  protected final Warehouse warehouse;
  protected final InputData input;
  private Position currentPosition;
  private String currentPickedProduct = null;

  // ----------------------------------------------------------------------------

  public Solution(final WarehouseInternal iwarehouse, final InputDataInternal iinput) {
    warehouse = iwarehouse;
    input = iinput;
    currentPosition = warehouse.getCurrentPosition();

    if (getParticipantName() == null) {
      throw new IllegalArgumentException("let getParticipantName() return your name");
    }
    if (getParticipantInstitution() == null) {
      throw new IllegalArgumentException("let getParticipantInstitution() return your institution");
    }
  }

  // ----------------------------------------------------------------------------

  /**
   * The main entry-point.
   */
  public void run() throws Exception {
    Collection<Order> orders = input.getAllOrders();
    List<Bin> allBins = warehouse.getAllBins();
    Map<String, List<Shelf>> productShelves = warehouse.getProductShelves();

    // Create a map to store nearest shelves for each product
    Map<String, Shelf> bestShelvesForProducts = findBestShelvesForProducts(productShelves);

    // Use a queue to process orders with better batching
    Queue<Order> orderQueue = new LinkedList<>(orders);

    // Main processing loop
    while (!orderQueue.isEmpty()) {
      Order currentOrder = findBestNextOrder(orderQueue, bestShelvesForProducts);
      orderQueue.remove(currentOrder);

      // Process this order
      processOrder(currentOrder, allBins, bestShelvesForProducts);
    }
  }

  /**
   * Process a single order completely
   */
  private void processOrder(Order order, List<Bin> allBins, Map<String, Shelf> bestShelvesForProducts) throws Exception {
    List<String> productsToProcess = new ArrayList<>(order.getOpenProducts());
    sortProductsByProximity(productsToProcess, bestShelvesForProducts);

    // Group products by their shelf locations
    Map<Position, List<String>> productGroups = new HashMap<>();
    for (String product : productsToProcess) {
      Position shelfPos = bestShelvesForProducts.get(product).getPosition();
      productGroups.computeIfAbsent(shelfPos, k -> new ArrayList<>()).add(product);
    }

    // Create clusters of nearby products
    List<List<String>> productClusters = new ArrayList<>();
    Position lastPos = currentPosition;
    List<String> currentCluster = new ArrayList<>();

    for (String product : productsToProcess) {
      Position productPos = bestShelvesForProducts.get(product).getPosition();

      // Start new cluster if distance is too large or different side
      if (!currentCluster.isEmpty() &&
              (warehouse.calcCost(lastPos, productPos) > 50 ||
                      lastPos.side != productPos.side)) {
        productClusters.add(new ArrayList<>(currentCluster));
        currentCluster.clear();
      }

      currentCluster.add(product);
      lastPos = productPos;
    }
    if (!currentCluster.isEmpty()) {
      productClusters.add(currentCluster);
    }

    // Process each cluster with its own bin
    Set<String> processedProducts = new HashSet<>();
    for (List<String> cluster : productClusters) {
      // Find closest available bin to the cluster's center
      Position clusterCenter = calculateClusterCenter(cluster, bestShelvesForProducts);
      Bin assignedBin = findClosestBin(allBins, clusterCenter);

      if (assignedBin != null) {
        warehouse.assignOrder(order, assignedBin);

        // Process all products in this cluster
        for (String product : cluster) {
          if (!processedProducts.contains(product)) {
            Shelf bestShelf = bestShelvesForProducts.get(product);

            // Count occurrences of this product in the cluster
            int neededCount = (int) cluster.stream().filter(p -> p.equals(product)).count();

            warehouse.pickProduct(bestShelf, product);
            currentPickedProduct = product;
            currentPosition = bestShelf.getPosition();

            // Put all instances of this product
            for (int i = 0; i < neededCount; i++) {
              warehouse.putProduct(assignedBin);
              currentPosition = assignedBin.getPosition();
            }

            processedProducts.add(product);
          }
        }
      }
    }

    // Finish the order once all products are processed
    warehouse.finishOrder(order);
  }

  private Position calculateClusterCenter(List<String> cluster, Map<String, Shelf> bestShelvesForProducts) {
    // Count shelves on each side
    int leftCount = 0;
    int rightCount = 0;
    int lengthwiseSum = 0;

    for (String product : cluster) {
      Shelf shelf = bestShelvesForProducts.get(product);
      if (shelf != null) {
        if (shelf.getPosition().side == Position.Side.Left) {
          leftCount++;
        } else {
          rightCount++;
        }
        lengthwiseSum += shelf.getPosition().lengthwise;
      }
    }

    // Determine the dominant side
    Position.Side centerSide = (leftCount > rightCount) ? Position.Side.Left : Position.Side.Right;

    // Calculate average lengthwise position
    int centerLengthwise = cluster.isEmpty() ? 1 : Math.max(1, lengthwiseSum / cluster.size());

    return new Position(centerSide, Position.Offset.Bin, centerLengthwise);
  }

  private Bin findClosestBin(List<Bin> allBins, Position referencePos) {
    return allBins.stream()
            .filter(bin -> warehouse.getOrderAssignedToBin(bin) == null)
            .min((b1, b2) -> Double.compare(
                    warehouse.calcCost(referencePos, b1.getPosition()),
                    warehouse.calcCost(referencePos, b2.getPosition())))
            .orElse(null);
  }

  /**
   * Sort products based on proximity to current position and to each other
   */
  private void sortProductsByProximity(List<String> products, Map<String, Shelf> bestShelvesForProducts) {
    Collections.sort(products, (p1, p2) -> {
      Position pos1 = bestShelvesForProducts.get(p1).getPosition();
      Position pos2 = bestShelvesForProducts.get(p2).getPosition();
      // Consider side changes in cost calculation
      double cost1 = getSideChangeCost(currentPosition, pos1) + warehouse.calcCost(currentPosition, pos1);
      double cost2 = getSideChangeCost(currentPosition, pos2) + warehouse.calcCost(currentPosition, pos2);
      return Double.compare(cost1, cost2);
    });
  }

  private double getSideChangeCost(Position from, Position to) {
    return from.side != to.side ? warehouse.getCostFactors().getSideChangeCost() : 0;
  }

  /**
   * Find the best order to process next based on proximity and simplicity
   */
  private Order findBestNextOrder(Queue<Order> orderQueue, Map<String, Shelf> bestShelvesForProducts) {
    Order bestOrder = null;
    double bestCost = Double.MAX_VALUE;

    for (Order order : orderQueue) {
      double orderCost = calculateOrderCost(order, bestShelvesForProducts);
      if (orderCost < bestCost) {
        bestCost = orderCost;
        bestOrder = order;
      }
    }

    return bestOrder != null ? bestOrder : orderQueue.peek();
  }

  /**
   * Calculate the cost of processing an order from current position
   */
  private double calculateOrderCost(Order order, Map<String, Shelf> bestShelvesForProducts) {
    double totalCost = 0;
    Position lastPosition = currentPosition;

    // Get unique products
    Set<String> uniqueProducts = new HashSet<>(order.getOpenProducts());

    for (String product : uniqueProducts) {
      Shelf shelf = bestShelvesForProducts.get(product);
      totalCost += warehouse.calcCost(lastPosition, shelf.getPosition());
      lastPosition = shelf.getPosition();
    }

    // Add cost for size/complexity of order (fewer products is better)
    totalCost += uniqueProducts.size() * 0.5;

    return totalCost;
  }

  /**
   * Find the best shelf for each product (closest to central position)
   */
  private Map<String, Shelf> findBestShelvesForProducts(Map<String, List<Shelf>> productShelves) {
    Map<String, Shelf> bestShelves = new HashMap<>();

    // Get a shelf from the warehouse to determine a good reference position
    Shelf anyShelf = null;
    for (List<Shelf> shelves : productShelves.values()) {
      if (!shelves.isEmpty()) {
        anyShelf = shelves.get(0);
        break;
      }
    }

    // Use current position as reference if available, otherwise use a position from a shelf
    Position referencePosition = warehouse.getCurrentPosition();

    for (Map.Entry<String, List<Shelf>> entry : productShelves.entrySet()) {
      String product = entry.getKey();
      List<Shelf> shelves = entry.getValue();

      Shelf bestShelf = null;
      double minCost = Double.MAX_VALUE;

      for (Shelf shelf : shelves) {
        double cost = warehouse.calcCost(referencePosition, shelf.getPosition());
        if (cost < minCost) {
          minCost = cost;
          bestShelf = shelf;
        }
      }

      if (bestShelf != null) {
        bestShelves.put(product, bestShelf);
      }
    }

    return bestShelves;
  }
}