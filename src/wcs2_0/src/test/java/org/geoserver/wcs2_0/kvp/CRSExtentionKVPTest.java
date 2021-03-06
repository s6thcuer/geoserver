package org.geoserver.wcs2_0.kvp;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;

import org.springframework.mock.web.MockHttpServletResponse;
/**
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class CRSExtentionKVPTest extends WCSKVPTestSupport {

    @Test
    public void capabilties() throws Exception {
        Document dom = getAsDOM("wcs?reQueSt=GetCapabilities&seErvIce=WCS");
         print(dom);
        
        // check the KVP extension 1.0.1
        assertXpathEvaluatesTo("1", "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs'])", dom);
        
        // proper case enforcing on values
        dom = getAsDOM("wcs?request=Getcapabilities&service=wCS");
        // print(dom);
        
        // check that we have the crs extension
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom); 
    }
    
    @Test
    public void reprojectTo3857() throws Exception {
        // subsample
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&&Format=image/tiff&OUTPUTCRS=http://www.opengis.net/def/crs/EPSG/0/3857");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);   

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            final CoordinateReferenceSystem targetCRS=CRS.decode("EPSG:3857", true);
            assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), targetCRS));            

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();
            
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{1.6308305401213994E7,-5543147.203861462},
                    new double[]{1.6475284637403902E7,-5311971.846945147});    
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            assertEquals(gridRange.getSpan(0), 360);
            assertEquals(gridRange.getSpan(1), 360);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
    
    @Test
    public void subsettingNativeCRSReprojectTo3857() throws Exception {

        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&&Format=image/tiff&" +
                        "OUTPUTCRS=http://www.opengis.net/def/crs/EPSG/0/3857&" +
                        "SUBSETTINGCRS=http://www.opengis.net/def/crs/EPSG/0/4326" +
                        "&subset=http://www.opengis.net/def/axis/OGC/0/Long(146.5,147.0)"+
                        "&subset=http://www.opengis.net/def/axis/OGC/0/Lat(-43.5,-43.0)");  

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);   

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            final CoordinateReferenceSystem targetCRS=CRS.decode("EPSG:3857", true);
            assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), targetCRS));            

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();
            
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{1.6308305401213994E7,-5388389.272818998},
                    new double[]{1.636396514661063E7,-5311971.846945147});    
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            assertEquals(gridRange.getSpan(0), 120);
            assertEquals(gridRange.getSpan(1), 120);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }       
    }
    
    @Test
    public void implicitRerpojectionTo3857() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&" +
                "SUBSETTINGCRS=http://www.opengis.net/def/crs/EPSG/0/3857" +
                "&subset=http://www.opengis.net/def/axis/OGC/0/X(1.6308305401213994E7,1.6475284637403902E7)"+
                "&subset=http://www.opengis.net/def/axis/OGC/0/Y(-5543147.203861462,-5311971.846945147)");  
    
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);   
    
        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            final CoordinateReferenceSystem targetCRS=CRS.decode("EPSG:3857", true);
            assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), targetCRS));            
    
            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();
            
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{1.6308305401213994E7,-5543147.203861462},
                    new double[]{1.6475284637403902E7,-5311971.846945147});    
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);
    
            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            assertEquals(gridRange.getSpan(0), 360);
            assertEquals(gridRange.getSpan(1), 360);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }       
    }   
    
    @Test
    public void testGetCoverageSubsettingTrimCRS() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&" +
                "OUTPUTCRS=http://www.opengis.net/def/crs/EPSG/0/3857&" +
                "SUBSETTINGCRS=http://www.opengis.net/def/crs/EPSG/0/3857" +
                "&subset=http://www.opengis.net/def/axis/OGC/0/X(1.6308305401213994E7,1.6475284637403902E7)"+
                "&subset=http://www.opengis.net/def/axis/OGC/0/Y(-5543147.203861462,-5311971.846945147)");  
    
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);   
    
        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            final CoordinateReferenceSystem targetCRS=CRS.decode("EPSG:3857", true);
            assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), targetCRS));            
    
            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();
            
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{1.6308305401213994E7,-5543147.203861462},
                    new double[]{1.6475284637403902E7,-5311971.846945147});    
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);
    
            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            assertEquals(gridRange.getSpan(0), 360);
            assertEquals(gridRange.getSpan(1), 360);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }       
    }    
   
}
