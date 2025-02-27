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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.knapp.codingcontest.operations.CostFactors;
import com.knapp.codingcontest.operations.InfoSnapshot;

public class InfoSnapshotInternal implements InfoSnapshot {
  private static final long serialVersionUID = 1L;

  // ----------------------------------------------------------------------------

  private final int unfinishedProductCount;
  private final Map<OperationType, Long> operationCounts;

  //
  private final double unfinishedOrdersCost;
  private final double productAssignmentCost;

  private final double totalCost;

  private final long ass[] = { 0, 0, 0, 0, }; // pickedOrders, pickedProducts, missedOrders, missedProducts

  // ----------------------------------------------------------------------------

  InfoSnapshotInternal(final WarehouseInternal iwarehouse) {
    final InputDataInternal in = iwarehouse.iinput;
    final CostFactors c = iwarehouse.costFactors;

    //
    unfinishedProductCount = in.orders.values().stream().mapToInt(o -> o.getOpenProducts().size()).sum();
    operationCounts = iwarehouse.operations.stream()
        .map(o -> o.type())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    //
    unfinishedOrdersCost = unfinishedProductCount * c.getUnfinishedProductPenalty();
    productAssignmentCost = (getOperationCount(OperationType.AssignProduct) + getOperationCount(OperationType.RemoveProduct))
        * c.getProductAssignmentCost();

    totalCost = unfinishedOrdersCost + productAssignmentCost;

    iwarehouse.assignmentStats.stream().forEach(as -> {
      ass[0] += as.pickedOrders.size();
      ass[1] += as.pickedProducts;
      ass[2] += as.missedOrders.size();
      ass[3] += as.missedProducts;
    });
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

  // ............................................................................

  @Override
  public double getUnfinishedOrdersCost() {
    return unfinishedOrdersCost;
  }

  @Override
  public double getProductAssignmentCost() {
    return productAssignmentCost;
  }

  @Override
  public double getTotalCost() {
    return totalCost;
  }

  // ----------------------------------------------------------------------------

  @Override
  public long getMissedOrders() {
    return ass[2];
  }

  @Override
  public long getMissedProducts() {
    return ass[3];
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
    sb.append("]{unfinishedOrdersCost=")
        .append(unfinishedOrdersCost)
        .append(", productAssignmentCost=")
        .append(productAssignmentCost);

    sb.append("} => totalCost=").append(totalCost);

    return sb.toString();
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
