package eu.dnetlib.iis.wf.importer.infospace.converter;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;

import eu.dnetlib.data.proto.FieldTypeProtos.StringField;
import eu.dnetlib.data.proto.OafProtos.OafEntity;
import eu.dnetlib.iis.importer.schemas.Project;

/**
 * {@link OafEntity} containing project details to {@link Project} converter.
 * 
 * @author mhorst
 *
 */
public class ProjectConverter implements OafEntityToAvroConverter<Project> {

    protected static final Logger log = Logger.getLogger(ProjectConverter.class);

    private static final String FUNDER_FUNDING_SEPARATOR = "::";

    private static final Set<String> ACRONYM_SKIP_LOWERCASED_VALUES = new HashSet<String>(
            Arrays.asList("undefined", "unknown"));
    
    // ------------------------ LOGIC --------------------------
    
    @Override
    public Project convert(OafEntity oafEntity) throws IOException {
        Preconditions.checkNotNull(oafEntity);
        if (oafEntity.hasProject()) {
            eu.dnetlib.data.proto.ProjectProtos.Project sourceProject = oafEntity.getProject();
            if (sourceProject.hasMetadata()) {
                Project.Builder builder = Project.newBuilder();
                builder.setId(oafEntity.getId());
                StringField acronym = sourceProject.getMetadata().getAcronym();
                
                if (isAcronymValid(acronym)) {
                    builder.setProjectAcronym(acronym.getValue());
                }
                
                String projectGrantId = sourceProject.getMetadata().getCode().getValue();
                if (StringUtils.isNotBlank(projectGrantId)) {
                    builder.setProjectGrantId(projectGrantId);
                }
                
                String optional1 = sourceProject.getMetadata().getOptional1().getValue();
                if (StringUtils.isNotBlank(optional1)) {
                    builder.setOptional1(optional1);
                }
                
                String optional2 = sourceProject.getMetadata().getOptional2().getValue();
                if (StringUtils.isNotBlank(optional2)) {
                    builder.setOptional2(optional2);
                }
                
                String extractedFundingClass = extractFundingClass(
                        extractStringValues(sourceProject.getMetadata().getFundingtreeList()));
                if (StringUtils.isNotBlank(extractedFundingClass)) {
                    builder.setFundingClass(extractedFundingClass);
                }
                return isDataValid(builder)?builder.build():null;
            } else {
                log.error("skipping: no metadata for project " + oafEntity.getId());
                return null;
            }
        } else {
            log.error("skipping: no project for entity " + oafEntity.getId());
            return null;
        }
    }

    /**
     * Extracts funding class from funding tree defined as XML.
     * @throws IOException exception thrown when unable to parse XML document
     */
    public static String extractFundingClass(List<String> fundingTreeList) throws IOException {
        if (CollectionUtils.isNotEmpty(fundingTreeList)) {
            for (String fundingTreeXML : fundingTreeList) {
                if (StringUtils.isNotBlank(fundingTreeXML)) {
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    try {
                        DocumentBuilder builder = builderFactory.newDocumentBuilder();
                        Document xmlDocument = builder.parse(new InputSource(new StringReader(fundingTreeXML)));
                        XPath xPath = XPathFactory.newInstance().newXPath();
                        StringBuilder strBuilder = new StringBuilder();
                        strBuilder.append(xPath.compile("//funder/shortname").evaluate(xmlDocument));
                        strBuilder.append(FUNDER_FUNDING_SEPARATOR);
                        strBuilder.append(xPath.compile("//funding_level_0/name").evaluate(xmlDocument));
                        return strBuilder.toString();
                    } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
                        throw new IOException("exception occurred when processing xml: " + fundingTreeXML, e);
                    }
                }
            }
        }
        // fallback
        return null;
    }

    /**
     * Verifies whether acronym should be considered as valid.
     * @return true if valid, false otherwise
     */
    public static boolean isAcronymValid(String acronym) {
        return StringUtils.isNotBlank(acronym)
                && !ACRONYM_SKIP_LOWERCASED_VALUES.contains(acronym.trim().toLowerCase());
    }
    
    // ------------------------ PRIVATE --------------------------
    
    /**
     * Extracts string values from {@link StringField} list.
     */
    private static List<String> extractStringValues(List<StringField> source) {
        if (CollectionUtils.isNotEmpty(source)) {
            List<String> results = new ArrayList<String>(source.size());
            for (StringField currentField : source) {
                results.add(currentField.getValue());
            }
            return results;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Verifies whether acronym should be considered as valid.
     * @return true if valid, false otherwise
     */
    private static boolean isAcronymValid(StringField acronym) {
        return isAcronymValid(acronym.getValue());
    }
    
    private boolean isDataValid(Project.Builder builder) {
        return builder.hasFundingClass() || builder.hasProjectAcronym() || builder.hasProjectGrantId();
    }

}
