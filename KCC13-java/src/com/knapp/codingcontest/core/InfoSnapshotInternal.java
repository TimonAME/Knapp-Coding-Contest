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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.knapp.codingcontest.core.InputDataInternal.MyPosition;
import com.knapp.codingcontest.core.WarehouseInternal.Operation;
import com.knapp.codingcontest.data.Position;
import com.knapp.codingcontest.operations.CostFactors;
import com.knapp.codingcontest.operations.InfoSnapshot;

public class InfoSnapshotInternal implements InfoSnapshot {
  private static final long serialVersionUID = 1L;

  // ----------------------------------------------------------------------------

  private final MyPosition pos0;

  //
  private final int unfinishedProductCount;
  private final Map<OperationType, Long> operationCounts;

  //
  private final int distance;
  private final int countZoneChange;
  private final int countSideChange;

  // additional info
  private final int missedFinishOrders;
  private final int missedPutProducts;
  private final int redundantPicks;
  private final int noPutPicks;

  //
  private final double unfinishedOrdersCost;
  private final double distanceCost;
  private final double sideChangeCost;
  private final double totalCost;

  // ----------------------------------------------------------------------------

  InfoSnapshotInternal(final WarehouseInternal iwarehouse) {
    final InputDataInternal in = iwarehouse.iinput;
    final CostFactors c = iwarehouse.costFactors;

    pos0 = in.pos0;

    //
    unfinishedProductCount = in.orders.values().stream().mapToInt(o -> o.getOpenProducts().size()).sum();
    operationCounts = iwarehouse.operations.stream()
        .map(o -> o.type())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    //
    final int[] dzs = calcDZS(c, iwarehouse.result());
    distance = dzs[0];
    countZoneChange = dzs[1];
    countSideChange = dzs[2];

    //
    missedFinishOrders = iwarehouse.missedFinishOrders;
    missedPutProducts = iwarehouse.missedPutProducts;
    redundantPicks = iwarehouse.redundantPicks;
    noPutPicks = iwarehouse.noPutPicks;

    //
    distanceCost = distance * c.getDistanceCost();
    sideChangeCost = countSideChange * c.getSideChangeCost();
    unfinishedOrdersCost = unfinishedProductCount * c.getUnfinishedProductPenalty();
    totalCost = distanceCost + sideChangeCost + unfinishedOrdersCost;
  }

  // ----------------------------------------------------------------------------

  @Override
  public int getUnfinishedProductCount() {
    return unfinishedProductCount;
  }

  @Override
  public int getOperationCount(final OperationType type) {
    return operationCounts.getOrDefault(type, Long.valueOf(0L)).intValue();
  }

  @Override
  public int getDistance() {
    return distance;
  }

  public int getCountZoneChange() {
    return countZoneChange;
  }

  @Override
  public int getCountSideChange() {
    return countSideChange;
  }

  // ............................................................................

  public int getMissedFinishOrders() {
    return missedFinishOrders;
  }

  public int getMissedPutProducts() {
    return missedPutProducts;
  }

  public int getRedundantPicks() {
    return redundantPicks;
  }

  public int getNoPutPicks() {
    return noPutPicks;
  }

  // ............................................................................

  @Override
  public double getUnfinishedOrdersCost() {
    return unfinishedOrdersCost;
  }

  @Override
  public double getDistanceCost() {
    return distanceCost;
  }

  @Override
  public double getSideChangeCost() {
    return sideChangeCost;
  }

  @Override
  public double getTotalCost() {
    return totalCost;
  }

  // ----------------------------------------------------------------------------

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append("InfoSnapshot[").append("unfinishedProductCount=").append(unfinishedProductCount);
    for (final OperationType op : OperationType.values()) {
      sb.append(", ").append(op).append("=").append(getOperationCount(op));
    }

    //
    sb.append("]{unfinishedOrdersCost=").append(unfinishedOrdersCost);
    sb.append(",distanceCost=").append(distanceCost);
    sb.append(",sideChangeCost=").append(sideChangeCost);

    sb.append("} => totalCost=").append(totalCost);

    return sb.toString();
  }

  // ----------------------------------------------------------------------------

  private int[] calcDZS(final CostFactors c, final List<Operation> result) {
    final int[] dzs = { 0, 0, 0, }; // dist, #zone, #side
    MyPosition pos_ = pos0;
    for (final Operation op : result) {
      switch (op.type()) {
        case AssignOrder:
          break;

        case PickProduct: {
          final MyPosition pos = ((WarehouseInternal.Operation.PickProduct) op).shelf.getPosition();
          dzs(c, dzs, pos_, pos);
          pos_ = pos;
          break;
        }

        case PutProduct: {
          final MyPosition pos = ((WarehouseInternal.Operation.PutProduct) op).bin.getPosition();
          dzs(c, dzs, pos_, pos);
          pos_ = pos;
          break;
        }

        case FinishOrder:
          break;
      }
    }
    return dzs;
  }

  // ............................................................................

  private void dzs(final CostFactors c, final int[] dzs, final MyPosition pos_, final MyPosition pos) {
    if (pos_.offset == Position.Offset.Shelf) {
      dzs[0] += c.getDistShelfBin();
    }

    if (pos_.side == pos.side) {
      dzs[0] += Math.abs(pos_.lengthwise - pos.lengthwise);

      if (pos_.zone != pos.zone) {
        dzs[1]++;
      }
    } else {
      dzs[0] += (pos_.lengthwise + pos.lengthwise);

      dzs[1]++;
      dzs[2]++;
    }

    if (pos.offset == Position.Offset.Shelf) {
      dzs[0] += c.getDistShelfBin();
    }
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  <T1, T2> Tuple<T1, T2> t(final T1 v1, final T2 v2) {
    return new Tuple<>(v1, v2);
  }

  public static final class Tuple<T1, T2> implements Serializable {
    private static final long serialVersionUID = 1L;

    public final T1 v1;
    public final T2 v2;

    public Tuple(final T1 v1, final T2 v2) {
      this.v1 = v1;
      this.v2 = v2;
    }

    public T1 v1() {
      return v1;
    }

    public T2 v2() {
      return v2;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = (prime * result) + ((v1 == null) ? 0 : v1.hashCode());
      return (prime * result) + ((v2 == null) ? 0 : v2.hashCode());
    }

    @Override
    public String toString() {
      return "Tuple[ " + this.v1 + " | " + this.v2 + " ]";
    }

    @Override
    public boolean equals(final Object other_) {
      if (!(other_ instanceof Tuple)) {
        return false;
      }
      final Tuple<?, ?> other = (Tuple<?, ?>) other_;
      return Tuple.isEqual(this.v1, other.v1) //
          && Tuple.isEqual(this.v2, other.v2);
    }

    private static boolean isEqual(final Object thisMember, final Object otherMember) {
      return (((thisMember == null) && (otherMember == null)) //
          || ((thisMember != null) && (thisMember.equals(otherMember))));
    }
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------
}
