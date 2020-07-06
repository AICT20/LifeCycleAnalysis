package soot.jimple.infoflow.results.xml;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import soot.SootClass;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.PatternDataHelper;
import soot.jimple.infoflow.pattern.patterndata.Pattern1Data;
import soot.jimple.infoflow.pattern.patterndata.Pattern2Data;
import soot.jimple.infoflow.pattern.patterndata.Pattern3Data;
import soot.jimple.infoflow.results.InfoflowPerformanceData;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

/**
 * Class for serializing FlowDroid results to XML
 * 
 * @author Steven Arzt
 *
 */
public class InfoflowResultsSerializer {

	public static final int FILE_FORMAT_VERSION = 102;

	protected boolean serializeTaintPath = true;
	protected IInfoflowCFG icfg;
	protected InfoflowConfiguration config;
	protected long startTime = 0;

	/**
	 * Creates a new instance of the InfoflowResultsSerializer class
	 */
	public InfoflowResultsSerializer() {
		this(null, null);
	}

	/**
	 * Creates a new instance of the InfoflowResultsSerializer class
	 * 
	 * @param cfg The control flow graph to be used for obtaining additional
	 *            information such as the methods containing source or sink
	 *            statements
	 */
	public InfoflowResultsSerializer(IInfoflowCFG cfg, InfoflowConfiguration config) {
		this.icfg = cfg;
		this.config = config;
	}

	/**
	 * Serializes the given FlowDroid result object into the given file
	 * 
	 * @param results  The result object to serialize
	 * @param fileName The target file name
	 * @throws FileNotFoundException Thrown if target file cannot be used
	 * @throws XMLStreamException    Thrown if the XML data cannot be written
	 */
	public void serialize(InfoflowResults results, String fileName) throws FileNotFoundException, XMLStreamException {
		this.startTime = System.currentTimeMillis();

		OutputStream out = new FileOutputStream(fileName);
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(out, "UTF-8");

		writer.writeStartDocument("UTF-8", "1.0");
		writer.writeStartElement(XmlConstants.Tags.root);
		writer.writeAttribute(XmlConstants.Attributes.fileFormatVersion, FILE_FORMAT_VERSION + "");
		writer.writeAttribute(XmlConstants.Attributes.terminationState,
				terminationStateToString(results.getTerminationState()));

		// Write out the data flow results
		if (results != null && !results.isEmpty()) {
			writer.writeStartElement(XmlConstants.Tags.results);
			writeDataFlows(results, writer);
			writer.writeEndElement();
		}
		writePatternDatas(writer);

		// Write out performance data
		InfoflowPerformanceData performanceData = results.getPerformanceData();
		if (performanceData != null && !performanceData.isEmpty()) {
			writer.writeStartElement(XmlConstants.Tags.performanceData);
			writePerformanceData(performanceData, writer);
			writer.writeEndElement();
		}

		writer.writeEndDocument();
		writer.close();
	}

	private void writePatternDatas(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement(XmlConstants.Tags.patterndatas);
		PatternDataHelper helper = PatternDataHelper.v();
		if (helper.hasPattern1()) {
			writer.writeStartElement(XmlConstants.Tags.patterndata);
			writer.writeAttribute(XmlConstants.Attributes.patterndatatype, "Pattern 1");
			Pattern1Data data = helper.getPattern1();
			Map<SootClass, String> entrypoints = data.getInvolvedEntrypoints();
			if (!entrypoints.isEmpty()) {
				writer.writeStartElement(XmlConstants.Tags.entrypoints);
				for (SootClass sootclass : entrypoints.keySet()) {
					writer.writeStartElement(XmlConstants.Tags.entrypoint);
					writer.writeAttribute(XmlConstants.Attributes.entrypointclass, sootclass.getName());
					writer.writeAttribute(XmlConstants.Attributes.entrypointposition, entrypoints.get(sootclass));
					writer.writeEndElement();
				}
				writer.writeEndElement();
			}

			writer.writeEndElement();
		}
		if (helper.hasPattern2()) {
			writer.writeStartElement(XmlConstants.Tags.patterndata);
			writer.writeAttribute(XmlConstants.Attributes.patterndatatype, "Pattern 2");
			Pattern2Data data = helper.getPattern2();
			writer.writeStartElement(XmlConstants.Tags.shouldCheck);
			writer.writeAttribute(XmlConstants.Attributes.shouldCheck, data.shouldCheck() + "");
			writer.writeAttribute(XmlConstants.Attributes.minSdk, data.getMinSdk() + "");
			writer.writeAttribute(XmlConstants.Attributes.targetSdk, data.getTargetSdk() + "");
			writer.writeEndElement();
		}
		if (helper.hasPattern3()) {
			Pattern3Data data = helper.getPattern3();
			writer.writeStartElement(XmlConstants.Tags.patterndata);
			writer.writeAttribute(XmlConstants.Attributes.patterndatatype, "Pattern 3");
			Map<String, Map<SootClass, Set<SootClass>>> map = data.getAllFragments();
			if (!map.isEmpty()) {
				for (String type : data.getAllFragments().keySet()) {
					Map<SootClass, Set<SootClass>> fragments = map.get(type);
					writer.writeStartElement(XmlConstants.Tags.fragments);
					writer.writeAttribute(XmlConstants.Attributes.fragmenttype, type);
					for (SootClass currentClass : fragments.keySet()) {
						writer.writeStartElement(XmlConstants.Tags.fragment);
						writer.writeAttribute(XmlConstants.Attributes.fragmentactivityclass, currentClass.getName());
						String fragmentnames = "";
						for (SootClass fragment : fragments.get(currentClass)) {
							fragmentnames += fragment.getName() + " ";
						}
						writer.writeAttribute(XmlConstants.Attributes.fragmentclasses, fragmentnames);
						writer.writeEndElement();
					}
					writer.writeEndElement();
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();

		}
		writer.writeEndElement();
	}

	/**
	 * Converts the termination state from the enumeration to a human-readable
	 * string
	 * 
	 * @param terminationState The termination state
	 * @return A human-readable version of the termination state
	 */
	private String terminationStateToString(int terminationState) {
		switch (terminationState) {
		case InfoflowResults.TERMINATION_SUCCESS:
			return "Success";
		case InfoflowResults.TERMINATION_DATA_FLOW_TIMEOUT:
			return "DataFlowTimeout";
		case InfoflowResults.TERMINATION_DATA_FLOW_OOM:
			return "DataFlowOutOfMemory";
		case InfoflowResults.TERMINATION_PATH_RECONSTRUCTION_TIMEOUT:
			return "PathReconstructionTimeout";
		case InfoflowResults.TERMINATION_PATH_RECONSTRUCTION_OOM:
			return "PathReconstructionOfMemory";
		default:
			return "Unknown";
		}
	}

	/**
	 * Writes out the given data flow performance data into the given XML stream
	 * writer
	 * 
	 * @param performanceData The performance data to write out
	 * @param writer          The stream writer into which to write the data
	 * @throws XMLStreamException Thrown if the XML data cannot be written
	 */
	private void writePerformanceData(InfoflowPerformanceData performanceData, XMLStreamWriter writer)
			throws XMLStreamException {
		writePerformanceEntry(XmlConstants.Values.PERF_CALLGRAPH_SECONDS,
				performanceData.getCallgraphConstructionSeconds(), writer);
		writePerformanceEntry(XmlConstants.Values.PERF_TAINT_PROPAGATION_SECONDS,
				performanceData.getTaintPropagationSeconds(), writer);
		writePerformanceEntry(XmlConstants.Values.PERF_PATH_RECONSTRUCTION_SECONDS,
				performanceData.getPathReconstructionSeconds(), writer);
		writePerformanceEntry(XmlConstants.Values.PERF_TOTAL_RUNTIME_SECONDS, performanceData.getTotalRuntimeSeconds(),
				writer);
		writePerformanceEntry(XmlConstants.Values.PERF_MAX_MEMORY_CONSUMPTION,
				performanceData.getMaxMemoryConsumption(), writer);

		writePerformanceEntry(XmlConstants.Values.PERF_SOURCE_COUNT, performanceData.getSourceCount(), writer);
		writePerformanceEntry(XmlConstants.Values.PERF_SINK_COUNT, performanceData.getSinkCount(), writer);
	}

	/**
	 * Writes a single performance data entry into the XML file. An entry has a name
	 * and a value, where the name describes the performance metric.
	 * 
	 * @param entryName  The name that describes the performance metric
	 * @param entryValue The value of the performance metric
	 * @param writer     The stream writer into which to write the data
	 * @throws XMLStreamException Thrown if the XML data cannot be written
	 */
	private void writePerformanceEntry(String entryName, int entryValue, XMLStreamWriter writer)
			throws XMLStreamException {
		if (entryValue > 0) {
			writer.writeStartElement(XmlConstants.Tags.performanceEntry);
			writer.writeAttribute(XmlConstants.Attributes.name, entryName);
			writer.writeAttribute(XmlConstants.Attributes.value, entryValue + "");
			writer.writeEndElement();
		}
	}

	/**
	 * Writes the given data flow results into the given XML stream writer
	 * 
	 * @param results The results to write out
	 * @param writer  The stream writer into which to write the results
	 * @throws XMLStreamException Thrown if the XML data cannot be written
	 */
	protected void writeDataFlows(InfoflowResults results, XMLStreamWriter writer) throws XMLStreamException {
		//lifecycle-add 修改成source->sink
//		for (ResultSinkInfo sink : results.getResults().keySet()) {
//			writer.writeStartElement(XmlConstants.Tags.result);
//			writeSinkInfo(sink, writer);
//
//			// Write out the sources
//			writer.writeStartElement(XmlConstants.Tags.sources);
//			for (ResultSourceInfo src : results.getResults().get(sink))
//				writeSourceInfo(src, writer);
//			writer.writeEndElement();
//
//			writer.writeEndElement();
//		}
		for (ResultSourceInfo source : results.getReResults().keySet()) {
			writer.writeStartElement(XmlConstants.Tags.result);
			writeSourceInfo(source, writer);

			// Write out the sources
			writer.writeStartElement(XmlConstants.Tags.sinks);
			for (ResultSinkInfo src : results.getReResults().get(source))
				writeSinkInfoWithPath(src, writer);
			writer.writeEndElement();

			writer.writeEndElement();
		}
	}

	/**
	 * Writes the given source information into the given XML stream writer
	 * 
	 * @param source The source information to write out
	 * @param writer The stream writer into which to write the results
	 * @throws XMLStreamException Thrown if the XML data cannot be written
	 */
	private void writeSourceInfo(ResultSourceInfo source, XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement(XmlConstants.Tags.source);
		writer.writeAttribute(XmlConstants.Attributes.statement, source.getStmt().toString());
		if (source.getDefinition().getCategory() != null)
			writer.writeAttribute(XmlConstants.Attributes.category,
					source.getDefinition().getCategory().getHumanReadableDescription());
		if (icfg != null) {
			writer.writeAttribute(XmlConstants.Attributes.method, icfg.getMethodOf(source.getStmt()).getSignature());
//			writer.writeAttribute(XmlConstants.Attributes.methodbody, icfg.getMethodOf(source.getStmt()).getActiveBody().toString());
		}

		writeAdditionalSourceInfo(source, writer);
		writeAccessPath(source.getAccessPath(), writer);

		if (serializeTaintPath && source.getPath() != null) {
			writer.writeStartElement(XmlConstants.Tags.taintPath);
			for (int i = 0; i < source.getPath().length; i++) {
				writer.writeStartElement(XmlConstants.Tags.pathElement);

				Stmt curStmt = source.getPath()[i];
				writer.writeAttribute(XmlConstants.Attributes.statement, curStmt.toString());

				if (icfg != null) {
					writer.writeAttribute(XmlConstants.Attributes.method, icfg.getMethodOf(curStmt).getSignature());
//					writer.writeAttribute(XmlConstants.Attributes.methodbody, icfg.getMethodOf(curStmt).getActiveBody().toString());
				}

				AccessPath curAP = source.getPathAccessPaths()[i];
				writeAccessPath(curAP, writer);

				writer.writeEndElement();
			}
			writer.writeEndElement();
		}

		writer.writeEndElement();
	}

	private void writeSinkInfoWithPath(ResultSinkInfo sink, XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement(XmlConstants.Tags.sink);
		writer.writeAttribute(XmlConstants.Attributes.statement, sink.getStmt().toString());
		if (sink.getDefinition().getCategory() != null)
			writer.writeAttribute(XmlConstants.Attributes.category,
					sink.getDefinition().getCategory().getHumanReadableDescription());
		if (icfg != null) {
			writer.writeAttribute(XmlConstants.Attributes.method, icfg.getMethodOf(sink.getStmt()).getSignature());
//			writer.writeAttribute(XmlConstants.Attributes.methodbody, icfg.getMethodOf(source.getStmt()).getActiveBody().toString());
		}

//		writeAdditionalSourceInfo(sink, writer);
		writeAccessPath(sink.getAccessPath(), writer);

		if (serializeTaintPath && sink.getPath() != null) {
			writer.writeStartElement(XmlConstants.Tags.taintPath);
			for (int i = 0; i < sink.getPath().length; i++) {
				writer.writeStartElement(XmlConstants.Tags.pathElement);

				Stmt curStmt = sink.getPath()[i];
				writer.writeAttribute(XmlConstants.Attributes.statement, curStmt.toString());

				if (icfg != null) {
					writer.writeAttribute(XmlConstants.Attributes.method, icfg.getMethodOf(curStmt).getSignature());
//					writer.writeAttribute(XmlConstants.Attributes.methodbody, icfg.getMethodOf(curStmt).getActiveBody().toString());
				}

				AccessPath curAP = sink.getPathAccessPaths()[i];
				writeAccessPath(curAP, writer);

				writer.writeEndElement();
			}
			writer.writeEndElement();
		}

		writer.writeEndElement();
	}

	/**
	 * Derived classes can override this method to write out additional information
	 * about a data flow source
	 * 
	 * @param source The source information to write out
	 * @param writer The stream writer into which to write the results
	 * @throws XMLStreamException Thrown if the XML data cannot be written
	 */
	protected void writeAdditionalSourceInfo(ResultSourceInfo source, XMLStreamWriter writer)
			throws XMLStreamException {
		//
	}

	/**
	 * Writes the given sink information into the given XML stream writer
	 * 
	 * @param sink   The sink information to write out
	 * @param writer The stream writer into which to write the results
	 * @throws XMLStreamException Thrown if the XML data cannot be written
	 */
	private void writeSinkInfo(ResultSinkInfo sink, XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement(XmlConstants.Tags.sink);
		writer.writeAttribute(XmlConstants.Attributes.statement, sink.getStmt().toString());
		if (sink.getDefinition().getCategory() != null)
			writer.writeAttribute(XmlConstants.Attributes.category,
					sink.getDefinition().getCategory().getHumanReadableDescription());
		if (icfg != null)
			writer.writeAttribute(XmlConstants.Attributes.method, icfg.getMethodOf(sink.getStmt()).getSignature());
		writeAdditionalSinkInfo(sink, writer);
		writeAccessPath(sink.getAccessPath(), writer);
		writer.writeEndElement();
	}

	/**
	 * Derived classes can override this method to write out additional information
	 * about a data flow sink
	 * 
	 * @param sink   The sink information to write out
	 * @param writer The stream writer into which to write the results
	 * @throws XMLStreamException Thrown if the XML data cannot be written
	 */
	protected void writeAdditionalSinkInfo(ResultSinkInfo sink, XMLStreamWriter writer) throws XMLStreamException {
		//
	}

	/**
	 * Writes the given access path into the given XML stream writer
	 * 
	 * @param accessPath The access path to write out
	 * @param writer     The stream writer into which to write the data
	 * @throws XMLStreamException Thrown if the XML data cannot be written
	 */
	protected void writeAccessPath(AccessPath accessPath, XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement(XmlConstants.Tags.accessPath);

		if (accessPath.getPlainValue() != null)
			writer.writeAttribute(XmlConstants.Attributes.value, accessPath.getPlainValue().toString());
		if (accessPath.getBaseType() != null)
			writer.writeAttribute(XmlConstants.Attributes.type, accessPath.getBaseType().toString());
		writer.writeAttribute(XmlConstants.Attributes.taintSubFields,
				accessPath.getTaintSubFields() ? XmlConstants.Values.TRUE : XmlConstants.Values.FALSE);

		// Write out the fields
		if (accessPath.getFieldCount() > 0) {
			writer.writeStartElement(XmlConstants.Tags.fields);
			for (int i = 0; i < accessPath.getFieldCount(); i++) {
				writer.writeStartElement(XmlConstants.Tags.field);
				writer.writeAttribute(XmlConstants.Attributes.value, accessPath.getFields()[i].toString());
				writer.writeAttribute(XmlConstants.Attributes.type, accessPath.getFieldTypes()[i].toString());
				writer.writeEndElement();
			}
			writer.writeEndElement();
		}

		writer.writeEndElement();
	}

	/**
	 * Sets whether the taint propagation path shall be serialized along with the
	 * respective data flow result
	 * 
	 * @param serialize True if taint propagation paths shall be serialized,
	 *                  otherwise false
	 */
	public void setSerializeTaintPath(boolean serialize) {
		this.serializeTaintPath = serialize;
	}

}
