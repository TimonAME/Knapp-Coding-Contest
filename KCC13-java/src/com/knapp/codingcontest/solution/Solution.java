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
    // Assign a bin to this order
    Bin assignedBin = findAvailableBin(allBins);
    warehouse.assignOrder(order, assignedBin);

    // Process all products in this order
    List<String> productsToProcess = new ArrayList<>(order.getOpenProducts());

    // Sort products by shelf location to minimize travel distance
    sortProductsByProximity(productsToProcess, bestShelvesForProducts);

    Set<String> processedProducts = new HashSet<>();

    for (String product : productsToProcess) {
      if (!processedProducts.contains(product)) {
        // Find best shelf for this product
        Shelf bestShelf = bestShelvesForProducts.get(product);

        // Pick product from shelf
        warehouse.pickProduct(bestShelf, product);
        currentPickedProduct = product;
        currentPosition = bestShelf.getPosition();

        // Count how many of this product we need
        int neededCount = 0;
        for (String p : order.getOpenProducts()) {
          if (p.equals(product)) {
            neededCount++;
          }
        }

        // Put all instances of this product
        for (int i = 0; i < neededCount; i++) {
          warehouse.putProduct(assignedBin);
          currentPosition = assignedBin.getPosition();
        }

        processedProducts.add(product);
      }
    }

    // Finish the order once all products are processed
    warehouse.finishOrder(order);
  }

  /**
   * Sort products based on proximity to current position and to each other
   */
  private void sortProductsByProximity(List<String> products, Map<String, Shelf> bestShelvesForProducts) {
    final Position startPosition = currentPosition;

    Collections.sort(products, new Comparator<String>() {
      @Override
      public int compare(String p1, String p2) {
        Position pos1 = bestShelvesForProducts.get(p1).getPosition();
        Position pos2 = bestShelvesForProducts.get(p2).getPosition();
        double cost1 = warehouse.calcCost(startPosition, pos1);
        double cost2 = warehouse.calcCost(startPosition, pos2);

        return Double.compare(cost1, cost2);
      }
    });
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
   * Find an available bin for the order
   */
  private Bin findAvailableBin(List<Bin> allBins) {
    // Find the closest available bin
    Bin closestBin = null;
    double minCost = Double.MAX_VALUE;

    for (Bin bin : allBins) {
      if (warehouse.getOrderAssignedToBin(bin) == null) {
        double cost = warehouse.calcCost(currentPosition, bin.getPosition());
        if (cost < minCost) {
          minCost = cost;
          closestBin = bin;
        }
      }
    }

    return closestBin;
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