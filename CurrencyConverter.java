import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyConverter {
    private static final String API_BASE_URL = "https://api.frankfurter.dev/v1";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    // Frankfurter returns simple JSON; these patterns read currency codes and rates
    // without requiring an external JSON library.
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("\"([A-Z]{3})\"\\s*:");
    private static final Pattern DATE_PATTERN = Pattern.compile("\"date\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    public static void main(String[] args) {
        new CurrencyConverter().run();
    }

    private void run() {
        System.out.println("=== Currency Converter ===");
        System.out.println("Connecting to Frankfurter to check supported currencies...");

        try (Scanner scanner = new Scanner(System.in)) {
            Set<String> supportedCurrencies = getSupportedCurrencies();
            String sourceCurrency = readCurrencyCode(scanner, "Enter source currency code (for example, INR): ",
                    supportedCurrencies);
            String targetCurrency = readCurrencyCode(scanner, "Enter target currency code (for example, USD): ",
                    supportedCurrencies);
            BigDecimal amount = readAmount(scanner);

            ExchangeRate exchangeRate;
            if (sourceCurrency.equals(targetCurrency)) {
                exchangeRate = new ExchangeRate(BigDecimal.ONE, "same currency");
            } else {
                exchangeRate = getLatestExchangeRate(sourceCurrency, targetCurrency);
            }

            BigDecimal convertedAmount = amount.multiply(exchangeRate.rate());
            System.out.println();
            System.out.println("Conversion result:");
            System.out.println("Source amount: " + amount.toPlainString() + " " + sourceCurrency);
            System.out.println("Exchange rate: 1 " + sourceCurrency + " = "
                    + exchangeRate.rate().stripTrailingZeros().toPlainString() + " " + targetCurrency);
            System.out.println("Converted amount: "
                    + convertedAmount.stripTrailingZeros().toPlainString() + " " + targetCurrency);
            System.out.println("Rate date: " + exchangeRate.date());
        } catch (IllegalArgumentException e) {
            System.out.println("Input error: " + e.getMessage());
        } catch (ApiException e) {
            System.out.println("Currency service error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Network error: Could not reach the currency service. Check your internet connection "
                    + "and try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("The request was interrupted. Please try again.");
        }
    }

    // Load the API's currency list so unsupported three-letter codes are caught
    // before requesting an exchange rate.
    private Set<String> getSupportedCurrencies() throws IOException, InterruptedException {
        String response = sendGetRequest(API_BASE_URL + "/currencies");
        Matcher matcher = CURRENCY_CODE_PATTERN.matcher(response);
        Set<String> currencies = new HashSet<>();
        while (matcher.find()) {
            currencies.add(matcher.group(1));
        }

        if (currencies.isEmpty()) {
            throw new ApiException("The currency service returned an unexpected currency list.");
        }
        return currencies;
    }

    private String readCurrencyCode(Scanner scanner, String prompt, Set<String> supportedCurrencies) {
        System.out.print(prompt);
        String enteredCode = scanner.nextLine().trim().toUpperCase(Locale.ROOT);
        if (!enteredCode.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency codes must contain exactly three letters, such as INR or USD.");
        }
        if (!supportedCurrencies.contains(enteredCode)) {
            throw new IllegalArgumentException("The currency code " + enteredCode
                    + " is not supported by Frankfurter. Check the code and try again.");
        }
        return enteredCode;
    }

    private BigDecimal readAmount(Scanner scanner) {
        System.out.print("Enter amount to convert: ");
        String enteredAmount = scanner.nextLine().trim();
        try {
            BigDecimal amount = new BigDecimal(enteredAmount);
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("The amount must be greater than zero.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid number, such as 125.50.");
        }
    }

    private ExchangeRate getLatestExchangeRate(String sourceCurrency, String targetCurrency)
            throws IOException, InterruptedException {
        String url = API_BASE_URL + "/latest?from=" + sourceCurrency + "&to=" + targetCurrency;
        String response = sendGetRequest(url);

        Pattern ratePattern = Pattern.compile("\"rates\"\\s*:\\s*\\{\\s*\""
                + Pattern.quote(targetCurrency)
                + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)");
        Matcher rateMatcher = ratePattern.matcher(response);
        if (!rateMatcher.find()) {
            throw new ApiException("No exchange rate was returned for " + sourceCurrency + " to "
                    + targetCurrency + ". Please try again later.");
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(response);
        String rateDate = dateMatcher.find() ? dateMatcher.group(1) : "latest available";
        return new ExchangeRate(new BigDecimal(rateMatcher.group(1)), rateDate);
    }

    private String sendGetRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ApiException("Frankfurter returned HTTP " + response.statusCode()
                    + ". Please try again later.");
        }
        return response.body();
    }

    private record ExchangeRate(BigDecimal rate, String date) {
    }

    private static class ApiException extends IOException {
        private ApiException(String message) {
            super(message);
        }
    }
}
