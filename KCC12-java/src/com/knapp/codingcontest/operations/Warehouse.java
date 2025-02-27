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

public interface Warehouse {
  // ----------------------------------------------------------------------------

  /**
   * @return available work-station
   */
  WorkStation getWorkStation();

  /**
   * @return a snapshot of various information: costs so far, unfinished count, costs
   */
  InfoSnapshot getInfoSnapshot();

  /**
   * @return the cost factors used
   */
  CostFactors getCostFactors();

  // ----------------------------------------------------------------------------
}
