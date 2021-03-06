/*--- (C) 1999-2020 Techniker Krankenkasse ---*/

package de.tk.opensource.services.leistung.diga;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tk.opensource.services.leistung.diga.type.DigaVerzeichnis;

public class DigaFhirVerzeichnisRequester {

	private static final Logger LOG = LoggerFactory.getLogger(DigaFhirVerzeichnisRequester.class);

	private static String inputDir;

	private static String outputFileName;

	private static final String OUTPUT_FILE_NAME_DEFAULT = "DigaVerzeichnis.json";

	public static void main(String[] args) {

		CommandLineParser parser = new DefaultParser();
		try {

			// parse the command line arguments
			CommandLine line = parser.parse(createOptions(), args);

			if (line.hasOption("h")) {
				printHelp();
			}

			inputDir = line.getOptionValue("in");
			if (StringUtils.isEmpty(inputDir)) {
				printHelp();
			}

			outputFileName = line.getOptionValue("out");

			String[] leftOverArgs = line.getArgs();

			if (leftOverArgs.length != 0) {
				printHelp();
			}

		} catch (ParseException exp) {

			LOG.error("Parsing failed.  Reason: " + exp.getMessage());
			printHelp();
		}

		new DigaFhirVerzeichnisRequester().run();
	}

	public void run() {

		InputStream catalogEntriesInputStream = null;
		InputStream deviceDefinitionsInputStream = null;
		InputStream chargeItemDefinitionsInputStream = null;
		InputStream organizationsInputStream = null;
		try {
			catalogEntriesInputStream = new FileInputStream(new File(inputDir + "/CatalogEntries.xml"));
			deviceDefinitionsInputStream = new FileInputStream(new File(inputDir + "/DeviceDefinitions.xml"));
			chargeItemDefinitionsInputStream = new FileInputStream(new File(inputDir + "/ChargeItemDefinitions.xml"));
			organizationsInputStream = new FileInputStream(new File(inputDir + "/Organizations.xml"));

		} catch (FileNotFoundException e) {
			LOG.error(e.getMessage());
			System.exit(1);
		}

		DigaFhirVerzeichnisParser parser = new DigaFhirVerzeichnisParser();

		DigaVerzeichnis digaVerzeichnis =
			parser
				.withCatalogEntriesInput(catalogEntriesInputStream)
				.withDeviceDefinitionsInput(deviceDefinitionsInputStream)
				.withChargeItemsInput(chargeItemDefinitionsInputStream)
				.withOrganizationsInput(organizationsInputStream)
				.parse();

		printDigaVerzeichnis(digaVerzeichnis);
	}

	private void printDigaVerzeichnis(DigaVerzeichnis digaVerzeichnis) {

		ObjectMapper mapper = new ObjectMapper();
		String json;
		try {
			json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(digaVerzeichnis);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage());
		}

		try {

			if(StringUtils.equals("-", outputFileName)) {
				System.out.print(json);
			} else {
				outputFileName = StringUtils.defaultString(outputFileName, OUTPUT_FILE_NAME_DEFAULT);
				File outputFile = new File(outputFileName);
				if (outputFile.createNewFile()) {
					LOG.info("File created: " + outputFileName);
				} else {
					LOG.info("File '" + outputFileName + "' already exists. File will be overwritten.");
				}

				try(FileWriter myWriter = new FileWriter(outputFile)) {
					myWriter.write(json);
					myWriter.close();
				}
			}

		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
	}

	protected static Options createOptions() {
		Options options = new Options();
		Option help = new Option("h", "help", false, "prints the help");
		Option input = new Option("in", "input-dir", true, "directory with FHIR XML-input files");
		input.setRequired(true);
		Option output = new Option("out", "output-file", true, "output file for combined json data, \n- for stdout, default '" + OUTPUT_FILE_NAME_DEFAULT + "'");
		output.setRequired(false);

		options.addOption(help);
		options.addOption(input);
		options.addOption(output);

		return options;
	}

	protected static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar diga-api-fhir-adapter.jar [options]", createOptions());
		System.exit(0);
	}

}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
