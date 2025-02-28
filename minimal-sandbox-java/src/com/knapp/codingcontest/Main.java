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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;

public class Main {
  // ----------------------------------------------------------------------------

  public static void main(final String... args) throws Exception {
    System.out.println("KNAPP Coding Contest: STARTING...");

    System.out.println("... LOADING DATA ...");
    final String[] data = Main.loadData("data");

    System.out.println("... PROCESSING DATA/GENERATING OUTPUT ...");
    for (final String d : data) {
      System.out.println("  [process] " + d);
    }

    System.out.println("... WRITING OUTPUT/RESULT ...");
    Main.writeData(".", data);

    System.out.println("KNAPP Coding Contest: FINISHED");
  }

  // ----------------------------------------------------------------------------

  private static String[] loadData(final String dataPath) throws Exception {
    final List<String> data = new LinkedList<String>();
    final BufferedReader in = new BufferedReader(new FileReader(dataPath + File.separator + "dummy-input.csv"));
    for (String line = in.readLine(); line != null; line = in.readLine()) {
      data.add(line);
    }
    in.close();
    return data.toArray(new String[data.size()]);
  }

  private static void writeData(final String resultPath, final String[] data) throws Exception {
    final BufferedWriter out = new BufferedWriter(new FileWriter(resultPath + File.separator + "dummy-output.csv"));
    for (final String line : data) {
      out.write("PROCESSED INPUT;  " + line);
      out.newLine();
    }
    out.close();
  }
}
