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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.knapp.codingcontest.core.InputDataInternal.MyOrder;
import com.knapp.codingcontest.core.WarehouseInternal.Operation.AssignProduct;
import com.knapp.codingcontest.core.WarehouseInternal.Operation.PickOrder;
import com.knapp.codingcontest.core.WarehouseInternal.Operation.RemoveProduct;
import com.knapp.codingcontest.core.WarehouseInternal.Operation.StartOrder;
import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Product;
import com.knapp.codingcontest.operations.CostFactors;
import com.knapp.codingcontest.operations.InfoSnapshot;
import com.knapp.codingcontest.operations.Warehouse;
import com.knapp.codingcontest.operations.ex.DuplicateProductException;
import com.knapp.codingcontest.operations.ex.NoEmptyOrderSlotAvailableException;
import com.knapp.codingcontest.operations.ex.NoEmptyProductSlotAvailableException;
import com.knapp.codingcontest.operations.ex.NoSuchProductException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderLeftException;
import com.knapp.codingcontest.operations.ex.OrderAlreadyStartedException;
import com.knapp.codingcontest.operations.ex.OrderNotActiveException;
import com.knapp.codingcontest.operations.ex.ProductNotAssignedException;

public class WarehouseInternal implements Warehouse {
  final InputDataInternal iinput;
  final CostFactors costFactors;

  // current state
  final Map<String, Map<Product, AssignmentStat>> workStationAssignedProducts = new TreeMap<>();
  final Map<String, Map<String, MyOrder>> workStationActiveOrders = new TreeMap<>();

  // result for export
  final List<Operation> operations = new ArrayList<>();

  // ----------------------------------------------------------------------------

  public static final class AssignmentStat {
    public final Product _product;
    public Set<String> pickedOrders = new TreeSet<>();
    public long pickedProducts;
    public Set<String> missedOrders = new TreeSet<>();
    public long missedProducts;

    AssignmentStat(final Product _product) {
      this._product = _product;
    }
  }

  public final List<AssignmentStat> assignmentStats = new ArrayList<>();

  // ----------------------------------------------------------------------------

  public WarehouseInternal(final InputDataInternal iinput) {
    this.iinput = iinput;
    costFactors = iinput.getCostFactors();
  }

  @Override
  public String toString() {
    return "Warehouse[]";
  }

  // ----------------------------------------------------------------------------

  /**
   * @return available workbench(es)
   */
  @Override
  public WorkStationInternal getWorkStation() {
    return iinput.workStations.values().iterator().next(); // @dummy: have exactly one
  }

  /**
   * @return a snapshot of various information: costs so far, unfinished count
   */
  @Override
  public InfoSnapshotInternal getInfoSnapshot() {
    return new InfoSnapshotInternal(this);
  }

  /**
   * @return the cost factors used
   */
  @Override
  public CostFactors getCostFactors() {
    return costFactors;
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  protected Iterable<Operation> result() {
    return operations;
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  void startOrder(final WorkStationInternal workStation, final MyOrder order)
      throws OrderAlreadyStartedException, NoEmptyOrderSlotAvailableException {
    final StartOrder op = new Operation.StartOrder(workStation, order);
    checkOpStartOrder(op);
    apply(op);
    operations.add(op);
  }

  void assignProduct(final WorkStationInternal workStation, final Product product)
      throws NoSuchProductException, DuplicateProductException, NoEmptyProductSlotAvailableException {
    final AssignProduct op = new AssignProduct(workStation, product);
    checkOpAssignProduct(op);
    apply(op);
    operations.add(op);
  }

  void removeProduct(final WorkStationInternal workStation, final Product product)
      throws NoSuchProductException, ProductNotAssignedException {
    final RemoveProduct op = new Operation.RemoveProduct(workStation, product);
    checkOpRemoveProduct(op);
    apply(op);
    operations.add(op);
  }

  void pickOrder(final WorkStationInternal workStation, final MyOrder order, final Product product)
      throws OrderNotActiveException, NoSuchProductInOrderException, NoSuchProductInOrderLeftException,
      ProductNotAssignedException {
    final PickOrder op = new Operation.PickOrder(workStation, order, product);
    checkOpPickOrder(op);
    apply(op);
    operations.add(op);
  }

  // ............................................................................

  private void checkOpStartOrder(final StartOrder op) throws OrderAlreadyStartedException, NoEmptyOrderSlotAvailableException {
    checkSanityWorkStation(op.workStation);
    checkSanityOrder(op.order);
    if (op.order.isFinished()) {
      throw new OrderAlreadyStartedException("FINISHED: ", op);
    }
    if (activeOrders(op.workStation).containsKey(op.order.getCode())) {
      throw new OrderAlreadyStartedException("ACTIVE: ", op);
    }
    if (activeOrders(op.workStation).size() >= op.workStation.getOrderSlots()) {
      throw new NoEmptyOrderSlotAvailableException(op);
    }
  }

  private void checkOpAssignProduct(final AssignProduct op)
      throws NoSuchProductException, DuplicateProductException, NoEmptyProductSlotAvailableException {
    checkSanityWorkStation(op.workStation);
    checkSanityProduct(op.product);
    if (assignedProducts(op.workStation).containsKey(op.product)) {
      throw new DuplicateProductException(op);
    }
    if (assignedProducts(op.workStation).size() >= op.workStation.getProductSlots()) {
      throw new NoEmptyProductSlotAvailableException(op);
    }
  }

  private void checkOpRemoveProduct(final RemoveProduct op) throws NoSuchProductException, ProductNotAssignedException {
    checkSanityWorkStation(op.workStation);
    checkSanityProduct(op.product);
    if (!assignedProducts(op.workStation).containsKey(op.product)) {
      throw new ProductNotAssignedException(op);
    }
  }

  private void checkOpPickOrder(final PickOrder op) throws OrderNotActiveException, NoSuchProductInOrderException,
      NoSuchProductInOrderLeftException, ProductNotAssignedException {
    checkSanityWorkStation(op.workStation);
    checkOrder(op);
  }

  private void checkOrder(final PickOrder op) throws OrderNotActiveException, NoSuchProductInOrderException,
      NoSuchProductInOrderLeftException, ProductNotAssignedException {
    if (!activeOrders(op.workStation).containsKey(op.order.getCode())) {
      throw new OrderNotActiveException(op);
    }
    checkSanityOrder(op.order);
    checkSanityOrder(activeOrders(op.workStation).get(op.order.getCode()));
    if (!op.order.getAllProducts().contains(op.product)) {
      throw new NoSuchProductInOrderException(op);
    }
    if (!op.order.getOpenProducts().contains(op.product)) {
      throw new NoSuchProductInOrderLeftException(op);
    }
    if (!assignedProducts(op.workStation).containsKey(op.product)) {
      throw new ProductNotAssignedException(op);
    }
  }

  //

  private void checkSanityWorkStation(final WorkStationInternal workStation) {
    assert iinput.workStations.get(workStation.getCode()) == workStation;
  }

  private void checkSanityOrder(final Order order) {
    assert iinput.orders.get(order.getCode()) == order;
  }

  private void checkSanityProduct(final Product product) throws NoSuchProductException {
    if (!iinput.isValidProduct(product)) {
      throw new NoSuchProductException(product);
    }
  }

  // ....................................

  private void apply(final StartOrder op) {
    activeOrders(op.workStation).put(op.order.getCode(), op.order);
  }

  private void apply(final AssignProduct op) {
    assignedProducts(op.workStation).put(op.product, new AssignmentStat(op.product));
  }

  private void apply(final RemoveProduct op) {
    updateAssignmentStat(op);
    final AssignmentStat as = assignedProducts(op.workStation).remove(op.product);
    assignmentStats.add(as);
  }

  private void apply(final PickOrder op) {
    activeOrders(op.workStation).get(op.order.getCode()).pickedProduct(op.product);
    if (op.order.isFinished()) {
      activeOrders(op.workStation).remove(op.order.getCode());
    }
    updateAssignmentStat(op);
  }

  private void updateAssignmentStat(final RemoveProduct op) {
    final AssignmentStat as = assignedProducts(op.workStation).get(op.product);
    activeOrders(op.workStation).values().stream().forEach(o -> {
      final long count = o.getOpenProducts().stream().filter(p -> p.equals(op.product)).count();
      if (count > 0) {
        as.missedOrders.add(o.getCode());
        as.missedProducts += count;
      }
    });
  }

  private void updateAssignmentStat(final PickOrder op) {
    final AssignmentStat as = assignedProducts(op.workStation).get(op.product);
    as.pickedOrders.add(op.order.getCode());
    as.pickedProducts++;
  }

  public void finishAssignmentStats() {
    // updateAssignmentStat(RemoveProduct)
    workStationAssignedProducts.entrySet()
        .stream()
        .forEach(
            epa -> epa.getValue().keySet().stream().forEach(p -> updateAssignmentStat(new RemoveProduct(getWorkStation(), p))));
    workStationAssignedProducts.values().stream().flatMap(pa -> pa.values().stream()).forEach(as -> assignmentStats.add(as));
  }

  // ............................................................................

  Map<Product, AssignmentStat> assignedProducts(final WorkStationInternal workStation) {
    return workStationAssignedProducts.computeIfAbsent(workStation.code, c -> new TreeMap<>());
  }

  Map<String, MyOrder> activeOrders(final WorkStationInternal workStation) {
    return workStationActiveOrders.computeIfAbsent(workStation.code, k -> new TreeMap<>());
  }

  // ----------------------------------------------------------------------------

  public static abstract class Operation {
    public abstract InfoSnapshot.OperationType type();

    public abstract String toResultString();

    // ............................................................................

    public static class StartOrder extends Operation {
      public final WorkStationInternal workStation;
      public final MyOrder order;

      private StartOrder(final WorkStationInternal workStation, final MyOrder order) {
        this.workStation = workStation;
        this.order = order;
      }

      @Override
      public InfoSnapshot.OperationType type() {
        return InfoSnapshot.OperationType.StartOrder;
      }

      @Override
      public String toResultString() {
        return String.format("%s;%s;%s;", type(), workStation.code, order.getCode());
      }
    }

    public static class AssignProduct extends Operation {
      public final WorkStationInternal workStation;
      public final Product product;

      private AssignProduct(final WorkStationInternal workStation, final Product product) {
        this.workStation = workStation;
        this.product = product;
      }

      @Override
      public InfoSnapshot.OperationType type() {
        return InfoSnapshot.OperationType.AssignProduct;
      }

      @Override
      public String toResultString() {
        return String.format("%s;%s;%s;", type(), workStation.code, product.getCode());
      }
    }

    public static class RemoveProduct extends Operation {
      public final WorkStationInternal workStation;
      public final Product product;

      private RemoveProduct(final WorkStationInternal workStation, final Product product) {
        this.workStation = workStation;
        this.product = product;
      }

      @Override
      public InfoSnapshot.OperationType type() {
        return InfoSnapshot.OperationType.RemoveProduct;
      }

      @Override
      public String toResultString() {
        return String.format("%s;%s;%s;", type(), workStation.code, product.getCode());
      }
    }

    public static class PickOrder extends Operation {
      public final WorkStationInternal workStation;
      public final MyOrder order;
      public final Product product;

      private PickOrder(final WorkStationInternal workStation, final MyOrder order, final Product product) {
        this.workStation = workStation;
        this.order = order;
        this.product = product;
      }

      @Override
      public InfoSnapshot.OperationType type() {
        return InfoSnapshot.OperationType.PickOrder;
      }

      @Override
      public String toResultString() {
        return String.format("%s;%s;%s;%s;", type(), workStation.code, order.getCode(), product.getCode());
      }
    }
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------
}