package com.knapp.codingcontest.solution;

import java.util.*;

import com.knapp.codingcontest.data.InputData;
import com.knapp.codingcontest.data.Institute;
import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Product;
import com.knapp.codingcontest.operations.Warehouse;
import com.knapp.codingcontest.operations.WorkStation;

/**
 * Optimized Solution for KNAPP Coding Contest
 */
public class Solution {
  public String getParticipantName() {
    return "Your Name";  // Replace with your name
  }

  public Institute getParticipantInstitution() {
    return Institute.HTL_Rennweg_Wien;  // Replace with your institute
  }

  protected final Warehouse warehouse;
  protected final WorkStation workStation;
  protected final InputData input;

  // Maps to track product usage
  private Map<String, Set<Order>> productToOrdersMap = new HashMap<>();
  private Set<String> assignedProducts = new HashSet<>();

  public Solution(final Warehouse warehouse, final InputData input) {
    this.warehouse = warehouse;
    this.workStation = warehouse.getWorkStation();
    this.input = input;

    // Pre-calculate product to orders mapping
    for (Order order : input.getAllOrders()) {
      for (Product product : order.getAllProducts()) {
        productToOrdersMap.computeIfAbsent(product.getCode(), k -> new HashSet<>()).add(order);
      }
    }
  }

  public void run() throws Exception {
    Set<Order> activeOrders = new HashSet<>();
    PriorityQueue<Order> orderQueue = rankOrders();

    while (!orderQueue.isEmpty() || !activeOrders.isEmpty()) {
      // Start orders if slots available
      while (activeOrders.size() < workStation.getOrderSlots() && !orderQueue.isEmpty()) {
        Order order = orderQueue.poll();
        try {
          workStation.startOrder(order);
          activeOrders.add(order);
        } catch (Exception e) {
          // Skip if cannot start
        }
      }

      if (activeOrders.isEmpty()) break;

      // Try to process until no more progress
      boolean progress = true;
      while (progress) {
        progress = false;

        // Assign needed products if slots available
        if (assignedProducts.size() < workStation.getProductSlots()) {
          String bestProduct = findBestProductToAssign(activeOrders);
          if (bestProduct != null) {
            Product product = findProductByCode(bestProduct);
            try {
              workStation.assignProduct(product);
              assignedProducts.add(bestProduct);
              progress = true;
            } catch (Exception e) {
              // Skip if cannot assign
            }
          }
        }

        // Pick products for orders
        for (Order order : new ArrayList<>(activeOrders)) {
          if (order.isFinished()) {
            activeOrders.remove(order);
            progress = true;
            continue;
          }

          for (Product product : order.getOpenProducts()) {
            if (assignedProducts.contains(product.getCode())) {
              try {
                workStation.pickOrder(order, product);
                progress = true;
                break;
              } catch (Exception e) {
                // Skip if cannot pick
              }
            }
          }
        }

        // Remove unused products
        Set<String> neededProducts = new HashSet<>();
        for (Order order : activeOrders) {
          for (Product product : order.getOpenProducts()) {
            neededProducts.add(product.getCode());
          }
        }

        Set<String> productsToRemove = new HashSet<>(assignedProducts);
        productsToRemove.removeAll(neededProducts);

        for (String code : productsToRemove) {
          Product product = findAssignedProductByCode(code);
          if (product != null) {
            try {
              workStation.removeProduct(product);
              assignedProducts.remove(code);
              progress = true;
            } catch (Exception e) {
              // Skip if cannot remove
            }
          }
        }
      }

      // If stuck, try to make space by removing least useful product
      if (!progress && !activeOrders.isEmpty() && assignedProducts.size() >= workStation.getProductSlots()) {
        String leastUseful = findLeastUsefulProduct(activeOrders);
        if (leastUseful != null) {
          Product product = findAssignedProductByCode(leastUseful);
          if (product != null) {
            try {
              workStation.removeProduct(product);
              assignedProducts.remove(leastUseful);
            } catch (Exception e) {
              // Skip if cannot remove
            }
          }
        }
      }
    }
  }

  /**
   * Rank orders by efficiency (orders sharing more products with others are prioritized)
   */
  private PriorityQueue<Order> rankOrders() {
    Map<Order, Double> orderScores = new HashMap<>();

    for (Order order : input.getAllOrders()) {
      Set<String> productCodes = new HashSet<>();
      for (Product product : order.getAllProducts()) {
        productCodes.add(product.getCode());
      }

      // Calculate score based on product sharing with other orders
      double score = 0;
      for (String code : productCodes) {
        score += productToOrdersMap.get(code).size();
      }
      score /= productCodes.size(); // Normalize by order size

      orderScores.put(order, score);
    }

    // Create priority queue with orders sorted by score (higher first)
    PriorityQueue<Order> queue = new PriorityQueue<>((o1, o2) ->
            Double.compare(orderScores.get(o2), orderScores.get(o1)));
    queue.addAll(input.getAllOrders());

    return queue;
  }

  /**
   * Find product that will benefit the most active orders
   */
  private String findBestProductToAssign(Set<Order> activeOrders) {
    Map<String, Integer> productUsage = new HashMap<>();

    for (Order order : activeOrders) {
      Set<String> orderProducts = new HashSet<>();
      for (Product product : order.getOpenProducts()) {
        if (!assignedProducts.contains(product.getCode())) {
          orderProducts.add(product.getCode());
        }
      }

      for (String code : orderProducts) {
        productUsage.put(code, productUsage.getOrDefault(code, 0) + 1);
      }
    }

    // Find product with highest usage
    String bestProduct = null;
    int maxUsage = 0;

    for (Map.Entry<String, Integer> entry : productUsage.entrySet()) {
      if (entry.getValue() > maxUsage) {
        maxUsage = entry.getValue();
        bestProduct = entry.getKey();
      }
    }

    return bestProduct;
  }

  /**
   * Find least useful product among currently assigned products
   */
  private String findLeastUsefulProduct(Set<Order> activeOrders) {
    Map<String, Integer> productUsage = new HashMap<>();

    // Initialize all assigned products with zero count
    for (String code : assignedProducts) {
      productUsage.put(code, 0);
    }

    // Count usages in active orders
    for (Order order : activeOrders) {
      for (Product product : order.getOpenProducts()) {
        String code = product.getCode();
        if (assignedProducts.contains(code)) {
          productUsage.put(code, productUsage.get(code) + 1);
        }
      }
    }

    // Find product with lowest usage
    String leastUseful = null;
    int minUsage = Integer.MAX_VALUE;

    for (Map.Entry<String, Integer> entry : productUsage.entrySet()) {
      if (entry.getValue() < minUsage) {
        minUsage = entry.getValue();
        leastUseful = entry.getKey();
      }
    }

    return leastUseful;
  }

  /**
   * Find any product instance with specified code
   */
  private Product findProductByCode(String code) {
    for (Order order : input.getAllOrders()) {
      for (Product product : order.getAllProducts()) {
        if (product.getCode().equals(code)) {
          return product;
        }
      }
    }
    return null;
  }

  /**
   * Find assigned product by code
   */
  private Product findAssignedProductByCode(String code) {
    for (Product product : workStation.getAssignedProducts()) {
      if (product.getCode().equals(code)) {
        return product;
      }
    }
    return null;
  }
}