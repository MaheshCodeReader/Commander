package com.crio.qcommerce.sale.insights;

import com.crio.qcommerce.contract.exceptions.AnalyticsException;
import com.crio.qcommerce.contract.insights.SaleAggregate;
import com.crio.qcommerce.contract.insights.SaleAggregateByMonth;
import com.crio.qcommerce.contract.insights.SaleInsights;
import com.crio.qcommerce.contract.resolver.DataProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class SaleInsightsImpl implements SaleInsights {

  @Override
  public SaleAggregate getSaleInsights(final DataProvider dataProvider, final int year)
      throws IOException, AnalyticsException {

    final File csvFile = dataProvider.resolveFile();
    final String vendorName = dataProvider.getProvider();

    final List<SaleAggregateByMonth> months = new ArrayList<>();
    for (int i = 1; i <= 12; i++) {
      months.add(new SaleAggregateByMonth(i, 0.0));
    }

    final BufferedReader br = Files.newBufferedReader(csvFile.toPath());
    final String delimiter = ",";
    String line = null;
    String[] cols = null;
    // read first line
    if (br != null) {
      line = br.readLine();
    }
    if (line != null) {
      cols = line.split(delimiter);
    }

    int dateIndex = -1;
    int amountIndex = -1;
    int statusIndex = -1;
    final String amazonStatus = "shipped";
    final String[] ebayFlipkartStatus = { "complete", "delivered", "paid", "shipped" };

    for (int i = 0; cols != null && i < cols.length; i++) {
      switch (cols[i]) {
        case "date":
        case "transaction_date":
          dateIndex = i;
          break;
        case "amount":
          amountIndex = i;
          break;
        case "status":
        case "transaction_status":
          statusIndex = i;
          break;
        default:
          break;
      }
    }

    while (br != null && (line = br.readLine()) != null) {

      // convert line into columns
      final String[] lineData = line.split(delimiter);

      // print all columns
      // System.out.println("User[" + String.join(", ", lineData) + "]");

      if (cols != null && lineData.length < cols.length
            || dateIndex == -1 || amountIndex == -1 || statusIndex == -1
            || lineData[dateIndex].equals("") || lineData[amountIndex].equals("")) {
        System.out.println("analytics exception will be thrown.");
        throw new AnalyticsException("Mahesh will see you in 5 mins");
      }

      if (lineData.length > amountIndex) {

        switch (vendorName) {
          case "amazon":
            if (lineData[statusIndex].equals(amazonStatus)) {
              int month = -1;
              int curYear = -1;
              final LocalDate date = LocalDate.parse(lineData[dateIndex]);
              if (date != null) {
                month = date.getMonthValue();
                curYear = date.getYear();
              }

              if (curYear == year) {
                final Double prevSales = months.get(month - 1).getSales();
                final Double curAmount = Double.parseDouble(lineData[amountIndex]);
                months.get(month - 1).setSales(prevSales + curAmount);
              }

            }
            break;
          case "flipkart":
          case "ebay":
            final String status = lineData[statusIndex].toLowerCase();
            if (status.equals(ebayFlipkartStatus[0]) || status.equals(ebayFlipkartStatus[1])
                || status.equals(ebayFlipkartStatus[2]) || status.equals(ebayFlipkartStatus[3])) {
              LocalDate date = null;
              switch (vendorName) {
                case "ebay":
                  final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/uuuu");
                  date = LocalDate.parse(lineData[dateIndex], dtf);
                  break;
                case "flipkart":
                  date = LocalDate.parse(lineData[dateIndex]);
                  break;
                default:
                  break;
              }
              int month = -1;
              int curYear = -1;
              if (date != null) {
                month = date.getMonthValue();
                curYear = date.getYear();
              }

              if (curYear == year) {
                final Double prevSales = months.get(month - 1).getSales();
                final Double curAmount = Double.parseDouble(lineData[amountIndex]);
                months.get(month - 1).setSales(prevSales + curAmount);
              }
            }

            break;
          default:
            break;
        }

        // if (vendorName.equals("amazon") &&
        // lineData[statusIndex].equals(amazonStatus)){
        // LocalDate date = LocalDate.parse(lineData[dateIndex]);
        // int month = date.getMonthValue();

        // Double prevSales = months.get(month).getSales();
        // Double curAmount = Double.parseDouble(lineData[amountIndex]);
        // months.get(month).setSales(prevSales + curAmount);

        // }
      }
    }

    Double totalSales = 0.0;
    for (int i = 0; i < months.size(); i++) {
      totalSales += months.get(i).getSales();
    }

    final SaleAggregate finalResult = new SaleAggregate();
    finalResult.setTotalSales(totalSales);
    finalResult.setAggregateByMonths(months);

    if (br != null) {
      br.close();
    }
    
    return finalResult;
  }
}