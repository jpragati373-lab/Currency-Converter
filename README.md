# Currency Converter

A beginner-friendly Java console application that converts an amount between two currencies using the latest available exchange rate from the Frankfurter public API.

## Features

- Reads source and target currency codes and an amount from the user.
- Checks currency codes against Frankfurter's supported-currency list.
- Fetches the latest available exchange rate at conversion time; rates are not hardcoded.
- Displays the source amount, exchange rate, converted amount, and rate date.
- Handles invalid input, unsupported currencies, API errors, and network interruptions with readable messages.
- Uses Java's built-in `HttpClient`; no third-party libraries are required.

## Prerequisites

- JDK 21 installed.
- A working internet connection.
- Visual Studio Code with the Extension Pack for Java (recommended for beginners).

## API used

[Frankfurter API](https://www.frankfurter.dev/) — a free, keyless exchange-rate API. The program reads the supported currencies from `/v1/currencies` and the latest rate from `/v1/latest`.

## Compile and run in VS Code on Windows

1. Open this project folder in VS Code.
2. Open **Terminal > New Terminal**. The commands below work in PowerShell when the terminal is in the project folder.
3. Confirm that Java 21 is installed and available in the terminal:

   ```powershell
   java -version
   javac -version
   ```

   Both commands should report version 21. If either command is not recognized, install a JDK 21 distribution and configure its `bin` directory on your Windows `PATH`, then restart VS Code.

4. Compile the program:

   ```powershell
   javac CurrencyConverter.java
   ```

5. Run it:

   ```powershell
   java CurrencyConverter
   ```

No dependency installation or module-path configuration is needed.

## Example tests

Run `java CurrencyConverter` once for each test, then enter the following values at the prompts:

### INR to USD

```text
Source currency: INR
Target currency: USD
Amount: 1000
```

### USD to INR

```text
Source currency: USD
Target currency: INR
Amount: 25
```

The actual rate and converted amount will depend on Frankfurter's latest available data and may differ between runs. Rates may not update on weekends or public holidays. Try an unsupported code such as `ZZZ`, a malformed code such as `US`, or a non-positive amount to see the input validation messages.
