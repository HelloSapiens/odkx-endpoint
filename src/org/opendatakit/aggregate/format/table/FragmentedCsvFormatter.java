/*
 * Copyright (C) 2010 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.aggregate.format.table;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.aggregate.constants.format.FormatConsts;
import org.opendatakit.aggregate.datamodel.FormElementModel;
import org.opendatakit.aggregate.form.Form;
import org.opendatakit.aggregate.format.Row;
import org.opendatakit.aggregate.format.SubmissionFormatter;
import org.opendatakit.aggregate.format.element.HtmlLinkElementFormatter;
import org.opendatakit.aggregate.submission.SubmissionKeyPart;
import org.opendatakit.aggregate.submission.SubmissionSet;
import org.opendatakit.common.constants.BasicConsts;
import org.opendatakit.common.constants.HtmlConsts;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;

/**
 * 
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public class FragmentedCsvFormatter extends TableFormatterBase implements SubmissionFormatter {
	private static final String PARENT_KEY_PROPERTY = "PARENT_KEY";
	private static final String RESULT_TABLE_KEY_STRING = "KEY";
	private static final String XML_TAG_NAMESPACE = "";
	private static final String XML_TAG_RESULT = "result";
	private static final String XML_TAG_HEADER = "header";
	private static final String XML_TAG_CURSOR = "cursor";
	private static final String XML_TAG_ENTRIES = "entries";

	private final String websafeCursorString;
	private final List<SubmissionKeyPart> submissionParts;
	
	public FragmentedCsvFormatter(Form xform, List<SubmissionKeyPart> submissionParts, String webServerUrl, String websafeCursorString, PrintWriter printWriter) {
		super(xform, printWriter, null);
		this.submissionParts = submissionParts;
		this.websafeCursorString = websafeCursorString;
	    elemFormatter = new HtmlLinkElementFormatter(webServerUrl, true, true, true);
	}

	/**
	 * Create the comma separated row with proper doubling of embedded quotes.
	 * 
	 * @param itr
	 *            string values to be separated by commas
	 * @return string containing comma separated values
	 */
	private String generateCommaSeperatedElements(List<String> elements) {
		StringBuilder row = new StringBuilder();
		boolean first = true;
		for ( String original : elements ) {
			// replace all quotes in the string with doubled-quotes
			// then wrap the whole thing with quotes.  Nulls are 
			// distinguished from empty strings by the lack of a 
			// value in that position (e.g., ,, vs ,"",)
			if ( !first ) {
				row.append(FormatConsts.CSV_DELIMITER);
			}
			first = false;
			if (original != null ) {
				row.append(BasicConsts.QUOTE);
				row.append(original.replace(BasicConsts.QUOTE, BasicConsts.QUOTE_QUOTE));
				row.append(BasicConsts.QUOTE);
			}
		}
		return row.toString();
	}

	private void emitXmlWrappedCsv(List<Row> resultTable, List<String> headers) throws IOException {

		Document d = new Document();
		d.setStandalone(true);
		d.setEncoding(HtmlConsts.UTF8_ENCODE);
		Element e = d.createElement(XML_TAG_NAMESPACE, XML_TAG_ENTRIES);
		d.addChild(0, Node.ELEMENT, e);
		int idx = 0;
		e.addChild(idx++, Node.IGNORABLE_WHITESPACE, BasicConsts.NEW_LINE);
		if ( websafeCursorString != null ) {
			Element cursor = d.createElement(XML_TAG_NAMESPACE, XML_TAG_CURSOR);
			e.addChild(idx++, Node.ELEMENT, cursor);
			cursor.addChild(0, Node.TEXT, websafeCursorString);
			e.addChild(idx++, Node.IGNORABLE_WHITESPACE, BasicConsts.NEW_LINE);
		}
		Element header = d.createElement(XML_TAG_NAMESPACE, XML_TAG_HEADER);
		e.addChild(idx++, Node.ELEMENT, header);
		header.addChild(0, Node.TEXT, generateCommaSeperatedElements(headers));
		e.addChild(idx++, Node.IGNORABLE_WHITESPACE, BasicConsts.NEW_LINE);

		Element resultRow;
		// generate rows
		for (Row row : resultTable) {
			resultRow = d.createElement(XML_TAG_NAMESPACE, XML_TAG_RESULT);
			e.addChild(idx++, Node.ELEMENT, resultRow);
			String csvRow = generateCommaSeperatedElements(row.getFormattedValues());
			resultRow.addChild(0, Node.TEXT, csvRow);
			e.addChild(idx++, Node.IGNORABLE_WHITESPACE, BasicConsts.NEW_LINE);
		}

		KXmlSerializer serializer = new KXmlSerializer();
		serializer.setOutput(output);
		// setting the response content type emits the xml header.
		// just write the body here...
		d.writeChildren(serializer); 
	}

	  @Override
	  public void processSubmissionSet(Collection<? extends SubmissionSet> submissions,
			  FormElementModel rootGroup) throws ODKDatastoreException {
		
	    List<Row> formattedElements = new ArrayList<Row>();
	    List<String> headers = headerFormatter.generateHeaders(form, rootGroup, propertyNames);

	    // format row elements 
	    for (SubmissionSet sub : submissions) {
	      Row row = sub.getFormattedValuesAsRow(propertyNames, elemFormatter, false);
	      formattedElements.add(row);
	    }
	    
	    try {
			emitXmlWrappedCsv(formattedElements, headers);
		} catch (IOException e) {
			e.printStackTrace();
		}
	  }
}