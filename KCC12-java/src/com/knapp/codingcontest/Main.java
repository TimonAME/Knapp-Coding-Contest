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

package com.knapp.codingcontest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.knapp.codingcontest.core.InfoSnapshotInternal;
import com.knapp.codingcontest.core.InputDataInternal;
import com.knapp.codingcontest.core.PrepareUpload;
import com.knapp.codingcontest.core.WarehouseInternal;
import com.knapp.codingcontest.operations.CostFactors;
import com.knapp.codingcontest.operations.InfoSnapshot;
import com.knapp.codingcontest.solution.Solution;

/**
 * ----------------------------------------------------------------------------
 * you may change any code you like
 *   => but changing the output may lead to invalid results on upload!
 * ----------------------------------------------------------------------------
 */
public class Main {
  // ----------------------------------------------------------------------------

  public static void main(final String... args) throws Exception {
    System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
    System.out.println("vvv   KNAPP Coding Contest: STARTING...        vvv");
    System.out.println(String.format("vvv                %s                    vvv", Main.DATE_FORMAT.format(new Date())));
    System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");

    System.out.println("# ------------------------------------------------");
    System.out.println("# ... LOADING INPUT ...");
    final CostFactors costFactors = new MainCostFactors();
    final InputDataInternal iinput = new InputDataInternal(costFactors);
    final WarehouseInternal iwarehouse = new WarehouseInternal(iinput);
    iinput.readData(iwarehouse);
    final InputDataInternal.InputStat istat = iinput.inputStat();

    System.out.println("# ------------------------------------------------");
    System.out.println("# ... RUN YOUR SOLUTION ...");
    final long start = System.currentTimeMillis();
    final Solution solution = new Solution(iwarehouse, iinput);
    Throwable throwable = null;
    try {
      solution.run();
    } catch (final Throwable _throwable) {
      throwable = _throwable;
    }
    iwarehouse.finishAssignmentStats();
    final long end = System.currentTimeMillis();
    System.out.println("# ... DONE ... (" + Main.formatInterval(end - start) + ")");

    System.out.println("# ------------------------------------------------");
    System.out.println("# ... WRITING OUTPUT/RESULT ...");
    PrepareUpload.createZipFile(solution, iwarehouse);
    System.out.println(">>> Created " + PrepareUpload.FILENAME_RESULT + " & " + PrepareUpload.uploadFileName(solution));

    System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    System.out.println("^^^   KNAPP Coding Contest: FINISHED           ^^^");
    System.out.println(String.format("^^^                %s                    ^^^", Main.DATE_FORMAT.format(new Date())));
    System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");

    System.out.println("# ------------------------------------------------");
    System.out.println("# ... RESULT/COSTS FOR YOUR SOLUTION ...");
    System.out.println("#     " + solution.getParticipantName() + " / " + solution.getParticipantInstitution());

    if (throwable != null) {
      System.out.println("");
      System.out.println("# ... Ooops ...");
      System.out.println("");
      throwable.printStackTrace(System.out);
    }

    Main.printResults(solution, istat, iwarehouse);
  }

  @SuppressWarnings("boxing")
  public static String formatInterval(final long interval) {
    final int h = (int) ((interval / (1000 * 60 * 60)) % 60);
    final int m = (int) ((interval / (1000 * 60)) % 60);
    final int s = (int) ((interval / 1000) % 60);
    final int ms = (int) (interval % 1000);
    return String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  @SuppressWarnings("boxing")
  public static void printResults(final Solution solution, final InputDataInternal.InputStat istat,
      final WarehouseInternal iwarehouse) throws Exception {
    final InfoSnapshotInternal info = iwarehouse.getInfoSnapshot();
    final CostFactors c = iwarehouse.getCostFactors();

    //
    final int os = info.getOperationCount(InfoSnapshot.OperationType.StartOrder);
    final int oa = info.getOperationCount(InfoSnapshot.OperationType.AssignProduct);
    final int or = info.getOperationCount(InfoSnapshot.OperationType.RemoveProduct);
    final int op = info.getOperationCount(InfoSnapshot.OperationType.PickOrder);

    //
    final int up = info.getUnfinishedProductCount();
    final double c_uo_ = info.getUnfinishedOrdersCost();
    final double c_a = info.getProductAssignmentCost();

    final double c_t = info.getTotalCost();

    //
    final long mo = info.getMissedOrders();
    final long mp = info.getMissedProducts();

    //
    System.out.println("# ------------------------------------------------");
    System.out.println("# ... RESULT/COSTS FOR YOUR SOLUTION ...");
    System.out.println("#     " + solution.getParticipantName() + " / " + solution.getParticipantInstitution());

    //
    System.out.println(String.format("  --------------------------------------------------------------"));
    System.out.println(String.format("    INPUT STATISTICS                                            "));
    System.out.println(String.format("  ------------------------------------- : ----------------------"));
    System.out.println(String.format("      #work-station                     :  %8d", istat.countWorkStations));
    System.out.println(String.format("      #orders                           :  %8d", istat.countOrders));
    System.out.println(String.format("      #products                         :  %8d", istat.countProducts));
    System.out.println(String.format("      #products (unique)                :  %8d", istat.countProductCodes));
    System.out.println(String.format("      products / order                  :  %10.1f", istat.avgProductPerOrder));

    //
    System.out.println(String.format("  --------------------------------------------------------------"));
    System.out.println(String.format("    RESULT STATISTICS                                           "));
    System.out.println(String.format("  ------------------------------------- : ----------------------"));
    System.out.println(String.format("    #operations"));
    System.out.println(String.format("      #start order                      :  %8d", os));
    System.out.println(String.format("      #assign product                   :  %8d", oa));
    System.out.println(String.format("      #remove product                   :  %8d", or));
    System.out.println(String.format("      #pick order/product               :  %8d", op));
    System.out.println(String.format("    missed opportunities"));
    System.out.println(String.format("      missed orders                     :  %8d", mo));
    System.out.println(String.format("      missed products                   :  %8d", mp));

    //
    System.out.println(String.format("  ============================================================================="));
    System.out.println(String.format("    RESULTS                                                                    "));
    System.out.println(String.format("  ===================================== : ============ | ======================"));
    System.out.println(String.format("      what                              :       costs  |  (details: count,...)"));
    System.out.println(String.format("  ------------------------------------- : ------------ | ----------------------"));
    System.out.println(String.format("   -> costs/unfinished products         :  %10.1f  |   %8d", c_uo_, up));
    System.out.println(String.format("   -> costs assign/remove               :  %10.1f  |   %8d", c_a, (oa + or)));
    System.out.println(String.format("  ------------------------------------- : ------------ | ----------------------"));
    System.out.println(String.format("   => TOTAL COST                           %10.1f", c_t));
    System.out.println(String.format("                                          ============"));
  }

  // ----------------------------------------------------------------------------

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

  // ----------------------------------------------------------------------------
}
