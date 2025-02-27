/* -*- java -*-
# =========================================================================== #
#                                                                             #
#                         Copyright (C) KNAPP AG                              #
#                                                                             #
#       The copyright to the computer program(s) herein is the property       #
#       of Knapp.  The program(s) may be used   and/or copied only with       #
#       the  written permission of  Knapp  or in  accordance  with  the       #
#       terms and conditions stipulated in the agreement/contract under       #
#       which the program(s) have been supplied.                              #
#                                                                             #
# =========================================================================== #
*/

package com.knapp.codingcontest.operations;

import java.util.Set;

import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Product;
import com.knapp.codingcontest.operations.ex.DuplicateProductException;
import com.knapp.codingcontest.operations.ex.NoEmptyOrderSlotAvailableException;
import com.knapp.codingcontest.operations.ex.NoEmptyProductSlotAvailableException;
import com.knapp.codingcontest.operations.ex.OrderNotActiveException;
import com.knapp.codingcontest.operations.ex.NoSuchProductException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderLeftException;
import com.knapp.codingcontest.operations.ex.OrderAlreadyStartedException;
import com.knapp.codingcontest.operations.ex.ProductNotAssignedException;

public interface WorkStation {
  // ----------------------------------------------------------------------------

  /**
   * @return code for work-station
   */
  String getCode();

  /**
  * @return number of maximal simultaneously allowed products
  */
  int getProductSlots();

  /**
   * @return number of maximal simultaneously allowed orders
   */
  int getOrderSlots();

  /**
   * @return products assigned to product slots at work-station
   */
  Set<Product> getAssignedProducts();

  /**
   * @return orders started at work-station
   */
  Set<Order> getActiveOrders();

  // ----------------------------------------------------------------------------

  /**
   * start working on an order on the work-station
   *
   * @param order
   */
  void startOrder(Order order) throws OrderAlreadyStartedException, NoEmptyOrderSlotAvailableException;

  /**
   * assign product to an empty slot on work-station
   *
   * @param product
   */
  void assignProduct(Product product)
      throws NoSuchProductException, DuplicateProductException, NoEmptyProductSlotAvailableException;

  /**
   * remove product from assigned slot on work-station
   *
   * @param product
   */
  void removeProduct(Product product) throws NoSuchProductException, ProductNotAssignedException;

  /**
   * pick product for order
   *
   * @param order
   * @param product
   */
  void pickOrder(Order order, Product product) throws OrderNotActiveException, NoSuchProductInOrderException,
      NoSuchProductInOrderLeftException, ProductNotAssignedException;

  // ----------------------------------------------------------------------------
}
