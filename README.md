# MyToolsCMD

Offline file conversion runner.

## Commands

Build:

```powershell
.\mvnw.cmd package
```

Run:

```powershell
java -jar target\mytools-0.0.1-SNAPSHOT.jar csv-to-xlsx --input data.csv --output data.xlsx
java -jar target\mytools-0.0.1-SNAPSHOT.jar xlsx-to-csv --input data.xlsx --output data.csv --sheet Sheet1
java -jar target\mytools-0.0.1-SNAPSHOT.jar format-xlsx-date --input data.xlsx --output data-formatted.xlsx --column Date --from dd/MM/yyyy --to yyyy-MM-dd
```

`format-xlsx-date` accepts `--column` as a header name, one-based column number, or Excel letters like `A`.
