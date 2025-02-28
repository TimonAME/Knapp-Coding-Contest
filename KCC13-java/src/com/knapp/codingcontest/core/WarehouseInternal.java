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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.knapp.codingcontest.core.InputDataInternal.MyBin;
import com.knapp.codingcontest.core.InputDataInternal.MyOrder;
import com.knapp.codingcontest.core.InputDataInternal.MyPosition;
import com.knapp.codingcontest.core.InputDataInternal.MyShelf;
import com.knapp.codingcontest.core.WarehouseInternal.Operation.AssignOrder;
import com.knapp.codingcontest.core.WarehouseInternal.Operation.FinishOrder;
import com.knapp.codingcontest.core.WarehouseInternal.Operation.PickProduct;
import com.knapp.codingcontest.core.WarehouseInternal.Operation.PutProduct;
import com.knapp.codingcontest.data.Bin;
import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Position;
import com.knapp.codingcontest.data.Shelf;
import com.knapp.codingcontest.operations.CostFactors;
import com.knapp.codingcontest.operations.InfoSnapshot;
import com.knapp.codingcontest.operations.InfoSnapshot.OperationType;
import com.knapp.codingcontest.operations.Warehouse;
import com.knapp.codingcontest.operations.ex.InvalidBinException;
import com.knapp.codingcontest.operations.ex.InvalidOrderException;
import com.knapp.codingcontest.operations.ex.InvalidProductException;
import com.knapp.codingcontest.operations.ex.InvalidShelfException;
import com.knapp.codingcontest.operations.ex.MpsBinAlreadyAssignedException;
import com.knapp.codingcontest.operations.ex.NoOrderAssignedToMpsBinException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderLeftException;
import com.knapp.codingcontest.operations.ex.OrderAlreadyReleasedException;
import com.knapp.codingcontest.operations.ex.OrderNotCompletelyPickedException;
import com.knapp.codingcontest.operations.ex.ProductNotFoundAtShelfException;

public class WarehouseInternal implements Warehouse {
  // ----------------------------------------------------------------------------

  // ----------------------------------------------------------------------------

  final InputDataInternal iinput;
  final CostFactors costFactors;

  // current state
  Map<String, MyOrder> orders = new TreeMap<>();
  Map<String, MyBin> bins = new TreeMap<>();
  Map<String, MyShelf> shelves = new TreeMap<>();
  //
  Map<String, String> bin4order = new TreeMap<>();
  String currentProduct;
  Set<String> finishedOrders = new TreeSet<>();

  private MyPosition currentPos;
  private Operation currentOp;

  // result for export
  final List<Operation> operations = new ArrayList<>();

  // ----------------------------------------------------------------------------

  // additional info
  int missedFinishOrders = 0;
  int missedPutProducts = 0;
  int redundantPicks = 0;
  int noPutPicks = 0;

  // ----------------------------------------------------------------------------

  public WarehouseInternal(final InputDataInternal iinput) {
    this.iinput = iinput;
    costFactors = iinput.getCostFactors();
    currentPos = iinput.pos0;
  }

  @Override
  public String toString() {
    return "Warehouse[]";
  }

  void prepareAfterRead() {
    for (final Order order : iinput.getAllOrders()) {
      orders.put(order.getCode(), (MyOrder) order);
    }
    for (final Bin bin : iinput.getAllBins()) {
      bins.put(bin.getCode(), (MyBin) bin);
    }
    iinput.shelves.values().stream().forEach(shelf -> shelves.put(shelf.getCode(), shelf));
  }

  // ----------------------------------------------------------------------------

  @Override
  public InfoSnapshotInternal getInfoSnapshot() {
    return new InfoSnapshotInternal(this);
  }

  @Override
  public CostFactors getCostFactors() {
    return costFactors;
  }

  @Override
  public Position getCurrentPosition() {
    return currentPos;
  }

  @Override
  public List<Bin> getAllBins() {
    return Collections.unmodifiableList(bins.values().stream().map(b -> (Bin) b).collect(Collectors.toList()));
  }

  @Override
  public List<Shelf> getAllShelves() {
    return Collections.unmodifiableList(shelves.values().stream().map(s -> (Shelf) s).collect(Collectors.toList()));
  }

  @Override
  public Map<String, List<Shelf>> getProductShelves() {
    return Collections.unmodifiableMap(iinput.productShelves);
  }

  @Override
  public Order getOrderAssignedToBin(final Bin bin) {
    final String ocode = bin4order.get(bin.getCode());
    return ocode != null ? orders.get(ocode) : null;
  }

  @Override
  public List<Bin> getBinsForOrder(final Order order) {
    return Collections.unmodifiableList(bin4order.entrySet()
        .stream()
        .filter(e -> order.getCode().equals(e.getValue()))
        .map(e -> bins.get(e.getKey()))
        .collect(Collectors.toList()));
  }

  @Override
  public double calcCost(final Position pos_, final Position pos) {
    double cost = 0.0;
    if (pos_.offset == Position.Offset.Shelf) {
      cost += (costFactors.getDistShelfBin() * costFactors.getDistanceCost());
    }
    if (pos_.side == pos.side) {
      cost += (Math.abs(pos_.lengthwise - pos.lengthwise) * costFactors.getDistanceCost());
    } else {
      cost += ((pos_.lengthwise + pos.lengthwise) * costFactors.getDistanceCost());
      cost += costFactors.getSideChangeCost();
    }
    if (pos.offset == Position.Offset.Shelf) {
      cost += (costFactors.getDistShelfBin() * costFactors.getDistanceCost());
    }
    return cost;
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  protected List<Operation> result() {
    return Collections.unmodifiableList(operations);
  }

  void setCurrentPos(final PickProduct op) {
    currentPos = op.myPosition();
    currentOp = op;
  }

  void setCurrentPos(final PutProduct op) {
    currentPos = op.myPosition();
    currentOp = op;
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  @Override
  public void assignOrder(final Order order, final Bin bin)
      throws InvalidOrderException, InvalidBinException, OrderAlreadyReleasedException, MpsBinAlreadyAssignedException {
    final Operation.AssignOrder op = new Operation.AssignOrder(order, bin);
    checkOp(op);
    applyOp(op);
    addOp(op);
  }

  @Override
  public void pickProduct(final Shelf shelf, final String product)
      throws InvalidShelfException, InvalidProductException, ProductNotFoundAtShelfException {
    final Operation.PickProduct op = new Operation.PickProduct(shelves.get(shelf.getCode()), product);
    checkOp(op);
    applyOp(op);
    addOp(op);
  }

  @Override
  public void putProduct(final Bin bin) throws InvalidBinException, NoSuchProductInOrderLeftException,
      NoSuchProductInOrderException, NoOrderAssignedToMpsBinException {
    final Operation.PutProduct op = new Operation.PutProduct(bins.get(bin.getCode()));
    checkOp(op);
    applyOp(op);
    addOp(op);
  }

  @Override
  public void finishOrder(final Order order) throws InvalidOrderException, OrderNotCompletelyPickedException {
    final Operation.FinishOrder op = new Operation.FinishOrder(order);
    checkOp(op);
    applyOp(op);
    addOp(op);
  }

  // ............................................................................

  private void checkOp(final AssignOrder op)
      throws InvalidOrderException, InvalidBinException, OrderAlreadyReleasedException, MpsBinAlreadyAssignedException {
    checkSanityOrder(op);
    checkSanityBin(op);
    if (isOrderFinished(op.order)) {
      throw new OrderAlreadyReleasedException(op);
    }
    // silently ignore subsequent assignments for same bin/order
    final MyOrder order = findOrderAssignedToBin(op.bin);
    if ((order != null) && (findOrder(op.order) != order)) {
      throw new MpsBinAlreadyAssignedException(op, order);
    }
  }

  private void checkOp(final PickProduct op)
      throws InvalidShelfException, InvalidProductException, ProductNotFoundAtShelfException {
    checkSanityShelf(op);
    checkSanityProduct(op);
    if (!op.shelf.getProducts().contains(op.product)) {
      throw new ProductNotFoundAtShelfException(op);
    }
  }

  private void checkOp(final PutProduct op) throws InvalidBinException, NoSuchProductInOrderLeftException,
      NoSuchProductInOrderException, NoOrderAssignedToMpsBinException {
    checkSanityBin(op);
    final MyOrder order = findOrderAssignedToBin(op.bin);
    if (order == null) {
      throw new NoOrderAssignedToMpsBinException(op);
    }
    if (!order.getAllProducts().contains(currentProduct)) {
      throw new NoSuchProductInOrderException(op, order, currentProduct);
    }
    if (!order.getOpenProducts().contains(currentProduct)) {
      throw new NoSuchProductInOrderLeftException(op, order, currentProduct);
    }
  }

  private void checkOp(final FinishOrder op) throws InvalidOrderException, OrderNotCompletelyPickedException {
    checkSanityOrder(op);
    final MyOrder order = findOrder(op.order);
    if (order == null) {
      return;
    }
    if (!order.getOpenProducts().isEmpty()) {
      throw new OrderNotCompletelyPickedException(op);
    }
  }

  // ............................................................................

  private void addOp(final AssignOrder op) {
    operations.add(op);
  }

  private void addOp(final PickProduct op) {
    operations.add(op);
  }

  private void addOp(final PutProduct op) {
    operations.add(op);
  }

  private void addOp(final FinishOrder op) {
    operations.add(op);
  }

  // ............................................................................

  private MyOrder findOrderAssignedToBin(final Bin bin) {
    final String ocode = bin4order.get(bin.getCode());
    if (ocode != null) {
      return orders.get(ocode);
    }
    return null;
  }

  private MyOrder findOrder(final Order order) {
    return orders.get(order.getCode());
  }

  // ....................................

  @Override
  public boolean isOrderFinished(final Order order) {
    return finishedOrders.contains(order.getCode());
  }

  // ............................................................................

  private void applyOp(final AssignOrder op) {
    updateAdditionalInfos(op);
    bin4order.put(op.bin.getCode(), op.order.getCode());
  }

  private void applyOp(final PickProduct op) {
    updateAdditionalInfos(op);
    currentProduct = op.product;
    setCurrentPos(op);
  }

  private void applyOp(final PutProduct op) {
    updateAdditionalInfos(op);
    final MyBin bin = bins.get(op.bin.getCode());
    final String ocode = bin4order.get(bin.getCode());
    final MyOrder order = orders.get(ocode);
    order.processedProduct(currentProduct);
    bin.putProduct(currentProduct);
    setCurrentPos(op);
  }

  private void applyOp(final FinishOrder op) {
    updateAdditionalInfos(op);
    finishedOrders.add(op.order.getCode());
    final String ocode = op.order.getCode();
    final List<String> bcodes = bin4order.entrySet()
        .stream()
        .filter(e -> ocode.equals(e.getValue()))
        .map(e -> e.getKey())
        .collect(Collectors.toList());
    for (final String bcode : bcodes) {
      bin4order.remove(bcode);
      final MyBin bin = bins.get(bcode);
      bin.finishBin();
    }
  }

  // ............................................................................

  private void checkSanityOrder(final AssignOrder op) throws InvalidOrderException {
    if (op.order == null) {
      throw new InvalidOrderException(op);
    }
  }

  private void checkSanityBin(final AssignOrder op) throws InvalidBinException {
    if (op.bin == null) {
      throw new InvalidBinException(op);
    }
  }

  private void checkSanityShelf(final PickProduct op) throws InvalidShelfException {
    if (op.shelf == null) {
      throw new InvalidShelfException(op);
    }
  }

  private void checkSanityProduct(final PickProduct op) throws InvalidProductException {
    if (op.product == null) {
      throw new InvalidProductException(op);
    }
  }

  private void checkSanityBin(final PutProduct op) throws InvalidBinException {
    if (op.bin == null) {
      throw new InvalidBinException(op);
    }
  }

  private void checkSanityOrder(final FinishOrder op) throws InvalidOrderException {
    if (op.order == null) {
      throw new InvalidOrderException(op);
    }
  }

  // ----------------------------------------------------------------------------

  // missedFinishOrder  ... any time put/assign is used before "finish-able" orders are finished (orders may be counted multiple times)
  // missedPutProduct   ... any move FROM last(!) bin when products could have been put
  // redundantPicks     ... subsequent picks to the same product, usually also means unnecessary distances are walked
  // noPutPicks         ... pick of a different product if no put has been done with previous
  private void updateAdditionalInfos(final Operation op) {
    if ((getCurrentPosition() == null) || (currentProduct == null)) {
      return;
    }

    if ((op.type() == OperationType.PutProduct) || (op.type() == OperationType.AssignOrder)) {
      final int count = (int) bin4order.values()
          .stream()
          .map(oc -> orders.get(oc))
          .distinct()
          .filter(o -> o.getOpenProducts().isEmpty())
          .count();
      missedFinishOrders += count;
    }

    if ((op.type() == OperationType.PickProduct) || (op.type() == OperationType.PutProduct)) {
      if (getCurrentPosition().offset == Position.Offset.Bin) {
        if (!equal((PutProduct) currentOp, op)) {
          final MyOrder order = findOrderAssignedToBin(((PutProduct) currentOp).bin);
          if ((order != null) && order.getOpenProducts().contains(currentProduct)) {
            missedPutProducts++;
          }
        }
      }
    }

    if (((op.type() == OperationType.PickProduct) && currentProduct.equals(((PickProduct) op).product))) {
      redundantPicks++;
    }

    if (((op.type() == OperationType.PickProduct) && (getCurrentPosition().offset == Position.Offset.Shelf))) {
      noPutPicks++;
    }
  }

  private boolean equal(final PutProduct op1, final Operation op2_) {
    if (!(op2_ instanceof PutProduct)) {
      return false;
    }
    final PutProduct op2 = (PutProduct) op2_;
    return op1.bin.getCode().equals(op2.bin.getCode());
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  public static abstract class Operation {
    public InfoSnapshot.OperationType type() {
      return null;
    }

    public abstract String toResultString();

    // ............................................................................

    public static class AssignOrder extends Operation {
      public final Order order;
      public final Bin bin;

      AssignOrder(final Order order, final Bin bin) {
        this.order = order;
        this.bin = bin;
      }

      @Override
      public InfoSnapshot.OperationType type() {
        return InfoSnapshot.OperationType.AssignOrder;
      }

      @Override
      public String toResultString() {
        return String.format("%s;%s;%s;", type(), order.getCode(), bin.getCode());
      }
    }

    // ............................................................................

    public static class PickProduct extends Operation {
      public final MyShelf shelf;
      public final String product;

      PickProduct(final MyShelf shelf, final String product) {
        this.shelf = shelf;
        this.product = product;
      }

      @Override
      public InfoSnapshot.OperationType type() {
        return InfoSnapshot.OperationType.PickProduct;
      }

      @Override
      public String toResultString() {
        return String.format("%s;%s;%s;", type(), shelf.getCode(), product);
      }

      MyPosition myPosition() {
        return shelf.getPosition();
      }
    }

    // ............................................................................

    public static class PutProduct extends Operation {
      public final MyBin bin;

      PutProduct(final MyBin bin) {
        this.bin = bin;
      }

      @Override
      public InfoSnapshot.OperationType type() {
        return InfoSnapshot.OperationType.PutProduct;
      }

      @Override
      public String toResultString() {
        return String.format("%s;%s;", type(), bin.getCode());
      }

      MyPosition myPosition() {
        return bin.getPosition();
      }
    }

    // ............................................................................

    public static class FinishOrder extends Operation {
      public final Order order;

      FinishOrder(final Order order) {
        this.order = order;
      }

      @Override
      public InfoSnapshot.OperationType type() {
        return InfoSnapshot.OperationType.FinishOrder;
      }

      @Override
      public String toResultString() {
        return String.format("%s;%s;", type(), order.getCode());
      }
    }
  }

  // ----------------------------------------------------------------------------
}