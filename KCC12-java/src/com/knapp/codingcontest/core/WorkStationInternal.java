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

package com.knapp.codingcontest.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.knapp.codingcontest.core.InputDataInternal.MyOrder;
import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Product;
import com.knapp.codingcontest.operations.WorkStation;
import com.knapp.codingcontest.operations.ex.DuplicateProductException;
import com.knapp.codingcontest.operations.ex.NoEmptyOrderSlotAvailableException;
import com.knapp.codingcontest.operations.ex.NoEmptyProductSlotAvailableException;
import com.knapp.codingcontest.operations.ex.OrderNotActiveException;
import com.knapp.codingcontest.operations.ex.NoSuchProductException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderLeftException;
import com.knapp.codingcontest.operations.ex.OrderAlreadyStartedException;
import com.knapp.codingcontest.operations.ex.ProductNotAssignedException;

public class WorkStationInternal implements WorkStation {
  private final WarehouseInternal iwarehouse;
  protected final String code;
  private final int productSlots;
  private final int orderSlots;

  // ----------------------------------------------------------------------------

  public WorkStationInternal(final WarehouseInternal imanufacture, final String code, final int productSlots,
      final int orderSlots) {
    iwarehouse = imanufacture;
    this.code = code;
    this.productSlots = productSlots;
    this.orderSlots = orderSlots;
  }

  // ----------------------------------------------------------------------------

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public int getProductSlots() {
    return productSlots;
  }

  @Override
  public int getOrderSlots() {
    return orderSlots;
  }

  @Override
  public Set<Product> getAssignedProducts() {
    return Collections.unmodifiableSet(iwarehouse.assignedProducts(this).keySet());
  }

  @Override
  public Set<Order> getActiveOrders() {
    return Collections.unmodifiableSet(new HashSet<>(iwarehouse.activeOrders(this).values()));
  }

  // ----------------------------------------------------------------------------

  @Override
  public String toString() {
    return code;
  }

  // ----------------------------------------------------------------------------

  @Override
  public void startOrder(final Order order) throws OrderAlreadyStartedException, NoEmptyOrderSlotAvailableException {
    iwarehouse.startOrder(this, (MyOrder) order);
  }

  @Override
  public void assignProduct(final Product product)
      throws NoSuchProductException, DuplicateProductException, NoEmptyProductSlotAvailableException {
    iwarehouse.assignProduct(this, product);
  }

  @Override
  public void removeProduct(final Product product) throws NoSuchProductException, ProductNotAssignedException {
    iwarehouse.removeProduct(this, product);
  }

  @Override
  public void pickOrder(final Order order, final Product product) throws OrderNotActiveException, NoSuchProductInOrderException,
      NoSuchProductInOrderLeftException, ProductNotAssignedException {
    iwarehouse.pickOrder(this, (MyOrder) order, product);
  }

  // ----------------------------------------------------------------------------
}
