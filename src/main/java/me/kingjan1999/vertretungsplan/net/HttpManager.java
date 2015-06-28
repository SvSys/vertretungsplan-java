package me.kingjan1999.vertretungsplan.net;

import me.kingjan1999.vertretungsplan.VMeta;
import me.kingjan1999.vertretungsplan.Vertretungsplan;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpManager {

    private final static String[] toRemove = new String[]{"</span", "<tr class='", "odd", "even", "<td class=\"list\"", "<th class=\"list\"", "align=\"center\"", "list", ">", "'", "</tr", "<span style=\"color: #010101\""};

    final String url;

    public HttpManager(String url) {
        this.url = url;
    }

    public Vertretungsplan fetchPlan() throws IOException {
        Document doc = Jsoup.connect(url).get();

        VMeta m = getMeta(doc);

        String[] infos = getInfos(doc);

        List<Map<String, String>> vertretungen = getVertretungen(doc);

        return new Vertretungsplan(m, infos, vertretungen);
        //return this.analysePlan(this.loadPlan());
    }

    private String[] getInfos(Document doc) {
        Element infotr = doc.select("table.info").select("tr").size() > 1 ? doc.select("table.info").select("tr").get(1) : null; //Skip "Nachrichten zum Tag"
        String[] infos = new String[0];
        if (infotr != null) {

            List<String> entries = new ArrayList<String>(Arrays.asList(infotr.html().split("<br>")));

            entries.removeIf(s -> s == null || s.trim().isEmpty()); //Remove empty / nulled entries

            infos = new String[entries.size()];
            for (int i = 0; i < infos.length; i++) {
                infos[i] = Jsoup.parseBodyFragment(entries.get(i)).text();
            }
        }
        return infos;
    }

    private VMeta getMeta(Document doc) {
        String untis_ver = doc.head().getElementsByAttributeValue("name", "generator").get(0).attributes().get("content");
        System.out.println(untis_ver);

        Element meta = doc.select(".mon_head").get(0).select("tr").get(0); //Get table with class mon_head -> First Row
        Elements tds = meta.select("td");

        String standW = tds.last().text(); //Erstes td -> Untis Version, Zweites TD -> ???, Drittes TD -> Stand ggf. Schulinformationen

        String stand = "Unbekannt";

        Pattern longDate = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}"); //Extract Date. Format: DD:MM:YYYY HH:mm (e.g. 26.06.2015 16:22)
        Matcher ml = longDate.matcher(standW);

        if (ml.find())
            stand = ml.group(0);

        String tagW = doc.select(".mon_title").get(0).text();
        String tag = "Unbekannt";

        System.out.println(tagW);
        Pattern shortDate = Pattern.compile("\\d{2}\\.\\d{1}\\.\\d{4}"); //Format: dd:M:YYYY (e.g. 29.6.2015)
        Matcher ms = shortDate.matcher(tagW);
        if (ms.find())
            tag = ms.group(0);

        Pattern page = Pattern.compile("(\\d) \\/ (\\d)"); //Format der Seiten: 1 / 2
        Matcher mp = page.matcher(tagW);

        int max = 1;
        int current = 1;

        if (mp.find() && mp.groupCount() > 1) {
            current = Integer.parseInt(mp.group(1));
            max = Integer.parseInt(mp.group(2));
        }

        return new VMeta(stand, untis_ver, tag, current, max);
    }

    private List<Map<String, String>> getVertretungen(Document doc) {
        List<String> cats = new ArrayList<String>(7);

        List<Map<String, String>> vf = new ArrayList<Map<String, String>>(20);

        Element vertretungsplan = doc.select(".mon_list").get(0);
        Elements vertretungen = vertretungsplan.select("tr");

        Elements headings = vertretungen.get(0).select("th");

        //Iterate headings
        for (Element heading : headings) {
            //System.out.println(heading.text());
            cats.add(heading.text());
        }

        for (int i = 1; i < vertretungen.size(); i++) {
            Elements cols = vertretungen.get(i).select("td");

            Map<String, String> eintrag = new HashMap<String, String>();

            for (int j = 0; j < cols.size(); j++) {
                eintrag.put(cats.get(j), cols.get(j).text());
            }
            //System.out.println(eintrag);
            vf.add(eintrag);
        }

        return vf;
    }

}
