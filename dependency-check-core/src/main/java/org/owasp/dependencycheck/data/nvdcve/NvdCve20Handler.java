/*
 * This file is part of dependency-check-core.
 *
 * Dependency-check-core is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Dependency-check-core is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * dependency-check-core. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.data.nvdcve;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.owasp.dependencycheck.data.cpe.CpeIndexWriter;
import org.owasp.dependencycheck.dependency.Reference;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX Handler that will parse the NVD CVE XML (schema version 2.0).
 *
 * @author Jeremy Long (jeremy.long@owasp.org)
 */
public class NvdCve20Handler extends DefaultHandler {

    /**
     * the current supported schema version.
     */
    private static final String CURRENT_SCHEMA_VERSION = "2.0";
    /**
     * the current element.
     */
    private final Element current = new Element();
    /**
     * the text of the node.
     */
    private StringBuilder nodeText;
    /**
     * the vulnerability.
     */
    private Vulnerability vulnerability;
    /**
     * a reference for the cve.
     */
    private Reference reference;
    /**
     * flag indicating whether the application has a cpe.
     */
    private boolean hasApplicationCpe = false;
    /**
     * The total number of entries parsed.
     */
    private int totalNumberOfEntries;

    /**
     * Get the value of totalNumberOfEntries.
     *
     * @return the value of totalNumberOfEntries
     */
    public int getTotalNumberOfEntries() {
        return totalNumberOfEntries;
    }
    /**
     * The total number of application entries parsed.
     */
    private int totalNumberOfApplicationEntries;

    /**
     * Get the value of totalNumberOfApplicationEntries.
     *
     * @return the value of totalNumberOfApplicationEntries
     */
    public int getTotalNumberOfApplicationEntries() {
        return totalNumberOfApplicationEntries;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        current.setNode(qName);
        if (current.isEntryNode()) {
            hasApplicationCpe = false;
            vulnerability = new Vulnerability();
            vulnerability.setName(attributes.getValue("id"));
        } else if (current.isVulnProductNode()) {
            nodeText = new StringBuilder(100);
        } else if (current.isVulnReferencesNode()) {
            final String lang = attributes.getValue("xml:lang");
            if ("en".equals(lang)) {
                reference = new Reference();
            } else {
                reference = null;
            }
        } else if (reference != null && current.isVulnReferenceNode()) {
            reference.setUrl(attributes.getValue("href"));
            nodeText = new StringBuilder(130);
        } else if (reference != null && current.isVulnSourceNode()) {
            nodeText = new StringBuilder(30);
        } else if (current.isVulnSummaryNode()) {
            nodeText = new StringBuilder(500);
        } else if (current.isNVDNode()) {
            final String nvdVer = attributes.getValue("nvd_xml_version");
            if (!CURRENT_SCHEMA_VERSION.equals(nvdVer)) {
                throw new SAXNotSupportedException("Schema version " + nvdVer + " is not supported");
            }
        } else if (current.isVulnCWENode()) {
            vulnerability.setCwe(attributes.getValue("id"));
        } else if (current.isCVSSScoreNode()) {
            nodeText = new StringBuilder(5);
        } else if (current.isCVSSAccessVectorNode()) {
            nodeText = new StringBuilder(20);
        } else if (current.isCVSSAccessComplexityNode()) {
            nodeText = new StringBuilder(20);
        } else if (current.isCVSSAuthenticationNode()) {
            nodeText = new StringBuilder(20);
        } else if (current.isCVSSAvailabilityImpactNode()) {
            nodeText = new StringBuilder(20);
        } else if (current.isCVSSConfidentialityImpactNode()) {
            nodeText = new StringBuilder(20);
        } else if (current.isCVSSIntegrityImpactNode()) {
            nodeText = new StringBuilder(20);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (nodeText != null) {
            nodeText.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        current.setNode(qName);
        if (current.isEntryNode()) {
            totalNumberOfEntries += 1;
            if (hasApplicationCpe) {
                totalNumberOfApplicationEntries += 1;
                try {
                    saveEntry(vulnerability);
                } catch (DatabaseException ex) {
                    throw new SAXException(ex);
                } catch (CorruptIndexException ex) {
                    throw new SAXException(ex);
                } catch (IOException ex) {
                    throw new SAXException(ex);
                }
            }
            vulnerability = null;
        } else if (current.isCVSSScoreNode()) {
            try {
                final float score = Float.parseFloat(nodeText.toString());
                vulnerability.setCvssScore(score);
            } catch (NumberFormatException ex) {
                Logger.getLogger(NvdCve20Handler.class.getName()).log(Level.SEVERE, "Error parsing CVSS Score.");
                Logger.getLogger(NvdCve20Handler.class.getName()).log(Level.FINE, null, ex);
            }
            nodeText = null;
        } else if (current.isCVSSAccessVectorNode()) {
            vulnerability.setCvssAccessVector(nodeText.toString());
            nodeText = null;
        } else if (current.isCVSSAccessComplexityNode()) {
            vulnerability.setCvssAccessComplexity(nodeText.toString());
            nodeText = null;
        } else if (current.isCVSSAuthenticationNode()) {
            vulnerability.setCvssAuthentication(nodeText.toString());
            nodeText = null;
        } else if (current.isCVSSAvailabilityImpactNode()) {
            vulnerability.setCvssAvailabilityImpact(nodeText.toString());
            nodeText = null;
        } else if (current.isCVSSConfidentialityImpactNode()) {
            vulnerability.setCvssConfidentialityImpact(nodeText.toString());
            nodeText = null;
        } else if (current.isCVSSIntegrityImpactNode()) {
            vulnerability.setCvssIntegrityImpact(nodeText.toString());
            nodeText = null;
        } else if (current.isVulnProductNode()) {
            final String cpe = nodeText.toString();
            if (cpe.startsWith("cpe:/a:")) {
                hasApplicationCpe = true;
                vulnerability.addVulnerableSoftware(cpe);
            }
            nodeText = null;
        } else if (reference != null && current.isVulnReferencesNode()) {
            vulnerability.addReference(reference);
            reference = null;
        } else if (reference != null && current.isVulnReferenceNode()) {
            reference.setName(nodeText.toString());
            nodeText = null;
        } else if (reference != null && current.isVulnSourceNode()) {
            reference.setSource(nodeText.toString());
            nodeText = null;
        } else if (current.isVulnSummaryNode()) {
            vulnerability.setDescription(nodeText.toString());
            nodeText = null;
        }
    }
    /**
     * the cve database.
     */
    private CveDB cveDB;

    /**
     * Sets the cveDB.
     *
     * @param db a reference to the CveDB
     */
    public void setCveDB(CveDB db) {
        cveDB = db;
    }
    /**
     * A list of CVE entries and associated VulnerableSoftware entries that
     * contain previous entries.
     */
    private Map<String, List<VulnerableSoftware>> prevVersionVulnMap;

    /**
     * Sets the prevVersionVulnMap.
     *
     * @param map the map of vulnerable software with previous versions being
     * vulnerable
     */
    public void setPrevVersionVulnMap(Map<String, List<VulnerableSoftware>> map) {
        prevVersionVulnMap = map;
    }

    /**
     * Saves a vulnerability to the CVE Database. This is a callback method
     * called by the Sax Parser Handler
     * {@link org.owasp.dependencycheck.data.nvdcve.xml.NvdCve20Handler}.
     *
     * @param vuln the vulnerability to store in the database
     * @throws DatabaseException thrown if there is an error writing to the
     * database
     * @throws CorruptIndexException is thrown if the CPE Index is corrupt
     * @throws IOException thrown if there is an IOException with the CPE Index
     */
    public void saveEntry(Vulnerability vuln) throws DatabaseException, CorruptIndexException, IOException {
        if (cveDB == null) {
            return;
        }
        final String cveName = vuln.getName();
        if (prevVersionVulnMap.containsKey(cveName)) {
            final List<VulnerableSoftware> vulnSoftware = prevVersionVulnMap.get(cveName);
            for (VulnerableSoftware vs : vulnSoftware) {
                vuln.updateVulnerableSoftware(vs);
            }
        }
        for (VulnerableSoftware vs : vuln.getVulnerableSoftware()) {
            if (cpeIndex != null) {
                cpeIndex.saveEntry(vs);
            }
        }
        cveDB.updateVulnerability(vuln);
    }
    /**
     * the cpe index.
     */
    private CpeIndexWriter cpeIndex;

    /**
     * Sets the cpe index writer.
     *
     * @param index the CPE Lucene Index
     */
    public void setCpeIndex(CpeIndexWriter index) {
        cpeIndex = index;
    }

    // <editor-fold defaultstate="collapsed" desc="The Element Class that maintains state information about the current node">
    /**
     * A simple class to maintain information about the current element while
     * parsing the NVD CVE XML.
     */
    protected static class Element {

        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String NVD = "nvd";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String ENTRY = "entry";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_PRODUCT = "vuln:product";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_REFERENCES = "vuln:references";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_SOURCE = "vuln:source";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_REFERENCE = "vuln:reference";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_SUMMARY = "vuln:summary";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_CWE = "vuln:cwe";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String CVSS_SCORE = "cvss:score";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String CVSS_ACCESS_VECTOR = "cvss:access-vector";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String CVSS_ACCESS_COMPLEXITY = "cvss:access-complexity";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String CVSS_AUTHENTICATION = "cvss:authentication";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String CVSS_CONFIDENTIALITY_IMPACT = "cvss:confidentiality-impact";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String CVSS_INTEGRITY_IMPACT = "cvss:integrity-impact";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String CVSS_AVAILABILITY_IMPACT = "cvss:availability-impact";
        /**
         * The current node.
         */
        private String node;

        /**
         * Gets the value of node.
         *
         * @return the value of node
         */
        public String getNode() {
            return this.node;
        }

        /**
         * Sets the value of node.
         *
         * @param node new value of node
         */
        public void setNode(String node) {
            this.node = node;
        }

        /**
         * Checks if the handler is at the NVD node.
         *
         * @return true or false
         */
        public boolean isNVDNode() {
            return NVD.equals(node);
        }

        /**
         * Checks if the handler is at the ENTRY node.
         *
         * @return true or false
         */
        public boolean isEntryNode() {
            return ENTRY.equals(node);
        }

        /**
         * Checks if the handler is at the VULN_PRODUCT node.
         *
         * @return true or false
         */
        public boolean isVulnProductNode() {
            return VULN_PRODUCT.equals(node);
        }

        /**
         * Checks if the handler is at the REFERENCES node.
         *
         * @return true or false
         */
        public boolean isVulnReferencesNode() {
            return VULN_REFERENCES.equals(node);
        }

        /**
         * Checks if the handler is at the REFERENCE node.
         *
         * @return true or false
         */
        public boolean isVulnReferenceNode() {
            return VULN_REFERENCE.equals(node);
        }

        /**
         * Checks if the handler is at the VULN_SOURCE node.
         *
         * @return true or false
         */
        public boolean isVulnSourceNode() {
            return VULN_SOURCE.equals(node);
        }

        /**
         * Checks if the handler is at the VULN_SUMMARY node.
         *
         * @return true or false
         */
        public boolean isVulnSummaryNode() {
            return VULN_SUMMARY.equals(node);
        }

        /**
         * Checks if the handler is at the VULN_CWE node.
         *
         * @return true or false
         */
        public boolean isVulnCWENode() {
            return VULN_CWE.equals(node);
        }

        /**
         * Checks if the handler is at the CVSS_SCORE node.
         *
         * @return true or false
         */
        public boolean isCVSSScoreNode() {
            return CVSS_SCORE.equals(node);
        }

        /**
         * Checks if the handler is at the CVSS_ACCESS_VECTOR node.
         *
         * @return true or false
         */
        public boolean isCVSSAccessVectorNode() {
            return CVSS_ACCESS_VECTOR.equals(node);
        }

        /**
         * Checks if the handler is at the CVSS_ACCESS_COMPLEXITY node.
         *
         * @return true or false
         */
        public boolean isCVSSAccessComplexityNode() {
            return CVSS_ACCESS_COMPLEXITY.equals(node);
        }

        /**
         * Checks if the handler is at the CVSS_AUTHENTICATION node.
         *
         * @return true or false
         */
        public boolean isCVSSAuthenticationNode() {
            return CVSS_AUTHENTICATION.equals(node);
        }

        /**
         * Checks if the handler is at the CVSS_CONFIDENTIALITY_IMPACT node.
         *
         * @return true or false
         */
        public boolean isCVSSConfidentialityImpactNode() {
            return CVSS_CONFIDENTIALITY_IMPACT.equals(node);
        }

        /**
         * Checks if the handler is at the CVSS_INTEGRITY_IMPACT node.
         *
         * @return true or false
         */
        public boolean isCVSSIntegrityImpactNode() {
            return CVSS_INTEGRITY_IMPACT.equals(node);
        }

        /**
         * Checks if the handler is at the CVSS_AVAILABILITY_IMPACT node.
         *
         * @return true or false
         */
        public boolean isCVSSAvailabilityImpactNode() {
            return CVSS_AVAILABILITY_IMPACT.equals(node);
        }
    }
    // </editor-fold>
}
