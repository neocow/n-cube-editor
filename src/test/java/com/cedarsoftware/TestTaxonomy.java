package com.cedarsoftware;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kpartlow on 10/8/2014.
 */
public class TestTaxonomy
{
    Pattern pattern = Pattern.compile("^[\\s]*([\\S]+)[\\s]+(.*)$");
    StringBuilder taxonomyColumns = new StringBuilder();
    StringBuilder taxonomyCells = new StringBuilder();

    StringBuilder taxonomyGroupColumns = new StringBuilder();
    StringBuilder taxonomyGroupCells = new StringBuilder();

    @Test
    public void testGenerateTaxonomy() throws Exception
    {


        StringBuilder g = new StringBuilder();

        initializeBuilders();
        readData();

        File dir = new File(TestTaxonomy.class.getClassLoader().getResource(".").toURI());

        writeOutTaxonomyCube(dir);
        writeOutTaxonomyGroups(dir);

    }


    private void writeOutTaxonomyGroups(File dir) throws IOException
    {
        StringBuilder b = new StringBuilder();
        b.append(getStartStringTaxonomyGroup());
        b.append(taxonomyGroupColumns.toString());
        b.append(getMidStringTaxonomyGroup());
        b.append(taxonomyGroupCells.toString());
        b.append(getLastStringTaxonomyGroup());
        try (FileOutputStream out = new FileOutputStream(new File(dir, "taxonomyGroups.json")))
        {
            out.write(b.toString().getBytes("UTF-8"));
        }
    }


    private void writeOutTaxonomyCube(File dir) throws IOException
    {
        StringBuilder b = new StringBuilder();
        b.append(getStartStringTaxonomy());
        b.append(taxonomyColumns.toString());
        b.append(getMidStringTaxonomy());
        b.append(taxonomyCells.toString());
        b.append(getLastStringTaxonomy());

        try (FileOutputStream out = new FileOutputStream(new File(dir, "taxonomy.json")))
        {
            out.write(b.toString().getBytes("UTF-8"));
        }
    }

    private void readData() throws IOException
    {
        int count = 0;
        try (InputStream in = new BufferedInputStream(TestTaxonomy.class.getClassLoader().getResourceAsStream("taxonomy.txt"), 32768))
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
            {
                String s = null;

                while ((s = reader.readLine()) != null)
                {
                    Matcher m = pattern.matcher(s);

                    if (m.matches())
                    {
                        String code = m.group(1).trim();
                        String desc = m.group(2).trim();
                        //System.out.println("Code:  " + code + " " + desc);

                        boolean parent = code.length() == 1;

                        if (parent)
                        {
                            addTaxonomyGroupColumn(code);
                            addTaxonomyGroupCell(code, desc);
                        }
                        else
                        {
                            addTaxonomyColumn(code);
                            addTaxonomyCell(count, code, desc);
                            count++;
                        }
                    }
                }
            }
        }
        removeLastCommas();
        System.out.println("Final Count:  " + count);
    }

    public void initializeBuilders()
    {
        taxonomyColumns.setLength(0);
        taxonomyCells.setLength(0);
        taxonomyGroupColumns.setLength(0);
        taxonomyGroupCells.setLength(0);
    }

    public String getStartStringTaxonomy()
    {
        return "{\n" +
                "  \"ncube\": \"rpm.enum.taxonomy.values\",\n" +
                "  \"defaultCellValueType\": \"string\",\n" +
                "  \"defaultCellValue\": \"#NOT_DEFINED\",\n" +
                "  \"requiredScopeKeys\": {\n" +
                "    \"type\": \"exp\",\n" +
                "    \"value\": \"['businessDivisionCode']\"\n" +
                "  },\n" +
                "  \"axes\": [\n" +
                "    {\n" +
                "      \"name\": \"name\",\n" +
                "      \"type\": \"DISCRETE\",\n" +
                "      \"valueType\": \"STRING\",\n" +
                "      \"preferredOrder\": 1,\n" +
                "      \"hasDefault\": false,\n" +
                "      \"columns\": [\n";
    }


    public String getStartStringTaxonomyGroup()
    {
        return "{\n" +
                "  \"ncube\": \"rpm.enum.taxonomy.group.table\",\n" +
                "  \"axes\": [\n" +
                "    {\n" +
                "      \"name\": \"group\",\n" +
                "      \"type\": \"DISCRETE\",\n" +
                "      \"valueType\": \"STRING\",\n" +
                "      \"preferredOrder\": 1,\n" +
                "      \"hasDefault\": false,\n" +
                "      \"columns\": [\n";
    }


    public String getMidStringTaxonomy()
    {
        return "]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"trait\",\n" +
                "      \"type\": \"DISCRETE\",\n" +
                "      \"valueType\": \"STRING\",\n" +
                "      \"preferredOrder\": 1,\n" +
                "      \"hasDefault\": false,\n" +
                "      \"columns\": [\n" +
                "        {\n" +
                "          \"id\": \"r:exists\",\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"r:ordinal\",\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"r:group\",\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"r:desc\",\n" +
                "          \"type\": \"string\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"cells\": [";
    }


    public String getMidStringTaxonomyGroup()
    {
        return "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"cells\": [";
    }

    public String getLastStringTaxonomyGroup()
    {
        return "]}";
    }


    public String getLastStringTaxonomy()
    {
        return "]}";
    }

    public void addTaxonomyColumn(String code)
    {
        taxonomyColumns.append("{");
        taxonomyColumns.append("\"id\": \"");
        taxonomyColumns.append(code);
        taxonomyColumns.append("\",");
        taxonomyColumns.append("\"type\": \"string\"");
        taxonomyColumns.append("},");
    }

    public void addTaxonomyCell(int ordinal, String code, String desc)
    {

        desc = desc.replace('�', '\'');
        taxonomyCells.append("{\"id\": [\"r:ordinal\",\"");
        taxonomyCells.append(code);
        taxonomyCells.append("\"],\"type\": \"long\",\"value\": ");
        taxonomyCells.append(ordinal);
        taxonomyCells.append("},");

        taxonomyCells.append("{\"id\": [\"r:desc\",\"");
        taxonomyCells.append(code);
        taxonomyCells.append("\"],\"type\": \"string\",\"value\": \"");
        taxonomyCells.append(desc);
        taxonomyCells.append("\"},");

        taxonomyCells.append("{\"id\": [\"r:group\",\"");
        taxonomyCells.append(code);
        taxonomyCells.append("\"],\"type\": \"exp\",\"value\": \"");
        taxonomyCells.append("@rpm.enum.taxonomy.group.table['group':input.name?.getAt(0)]");
        taxonomyCells.append("\"},");

        taxonomyCells.append("{\"id\": [\"r:exists\",\"");
        taxonomyCells.append(code);
        taxonomyCells.append("\"],\"type\": \"boolean\",\"value\": ");
        taxonomyCells.append(true);
        taxonomyCells.append("},");
    }


    public void addTaxonomyGroupColumn(String code) {
        taxonomyGroupColumns.append("{");
        taxonomyGroupColumns.append("\"id\": \"");
        taxonomyGroupColumns.append(code);
        taxonomyGroupColumns.append("\",");
        taxonomyGroupColumns.append("\"type\": \"string\"");
        taxonomyGroupColumns.append("},");
    }

    public void addTaxonomyGroupCell(String code, String desc) {
        taxonomyGroupCells.append("{\"id\": [\"");
        taxonomyGroupCells.append(code);
        taxonomyGroupCells.append("\"],\"type\": \"string\",\"value\": \"");
        taxonomyGroupCells.append(desc);
        taxonomyGroupCells.append("\"},");
    }

    public void removeLastCommas() {
        taxonomyColumns.setLength(taxonomyColumns.length()-1);
        taxonomyCells.setLength(taxonomyCells.length()-1);
        taxonomyGroupColumns.setLength(taxonomyGroupColumns.length()-1);
        taxonomyGroupCells.setLength(taxonomyGroupCells.length()-1);
    }
}
