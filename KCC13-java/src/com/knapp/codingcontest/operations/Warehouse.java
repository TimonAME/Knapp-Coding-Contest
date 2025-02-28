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

import java.util.List;
import java.util.Map;

import com.knapp.codingcontest.data.Bin;
import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Position;
import com.knapp.codingcontest.data.Shelf;
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

public interface Warehouse {
  // ----------------------------------------------------------------------------

  /**
   * @return the MPS-Bins (semi-automatic picksystem)
   */
  List<Bin> getAllBins();

  /**
   * @return all shelves
   */
  List<Shelf> getAllShelves();

  /**
   * @return the shelf(s) to pick products from
   */
  Map<String, List<Shelf>> getProductShelves();

  /**
   * @return a snapshot of various information: costs so far, unfinished count, costs
   */
  InfoSnapshot getInfoSnapshot();

  /**
   * @return the cost factors used
   */
  CostFactors getCostFactors();

  // ----------------------------------------------------------------------------

  void assignOrder(Order order, Bin bin)
      throws InvalidOrderException, InvalidBinException, OrderAlreadyReleasedException, MpsBinAlreadyAssignedException;

  void pickProduct(Shelf shelf, String product)
      throws InvalidShelfException, InvalidProductException, ProductNotFoundAtShelfException;

  void putProduct(Bin bin) throws InvalidBinException, NoSuchProductInOrderLeftException, NoSuchProductInOrderException,
      NoOrderAssignedToMpsBinException;

  void finishOrder(Order order) throws InvalidOrderException, OrderNotCompletelyPickedException;

  // ----------------------------------------------------------------------------

  /**
   *
   * @param bin
   * @return
   */
  Order getOrderAssignedToBin(Bin bin);

  /**
   *
   * @param order
   * @return
   */
  List<Bin> getBinsForOrder(Order order);

  /**
   * @return the current position (starts at Right,Bin,1)
   */
  Position getCurrentPosition();

  /**
   * Calculate the costs for a move between any 2 positions
   *
   * @param pos1
   * @param pos2
   * @return
   */
  double calcCost(Position pos1, Position pos2);

  /**
   *
   * @param order
   * @return
   */
  boolean isOrderFinished(Order order);
}
