package org.example.experiment;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Dependency-free PNG charts and medians, derived from the raw CSV. */
public final class Plotter {
    private static final Color BLUE = new Color(27, 91, 164);
    private static final Color RED = new Color(199, 64, 64);
    private static final Color GREEN = new Color(29, 132, 112);
    private static final Color GOLD = new Color(177, 118, 22);
    private record Key(String algorithm, String input, int n, int m) { }
    private record Summary(Key key, double median, double min, double max, long calls, long cells,
                           long comparisons, long lps, long allocated, int result) { }
    private record Point(double x, double y) { }
    private record Series(String label, Color color, boolean dashed, List<Point> points) { }
    private Plotter() { }

    public static void generate(Path output) throws IOException {
        List<Summary> data = summarize(output.resolve("results.csv"));
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(output.resolve("summary.csv")))) {
            csv.println("algorithm,input,n,m,median_time_ms,min_time_ms,max_time_ms,recursive_calls,cells_updated,char_comparisons,lps_comparisons,allocated_int_cells,result");
            for (Summary s : data) csv.printf(Locale.ROOT, "%s,%s,%d,%d,%.9f,%.9f,%.9f,%d,%d,%d,%d,%d,%d%n",
                    s.key.algorithm,s.key.input,s.key.n,s.key.m,s.median,s.min,s.max,s.calls,s.cells,s.comparisons,s.lps,s.allocated,s.result);
            if (csv.checkError()) throw new IOException("Failed to write summary.csv");
        }
        Path plots = output.resolve("plots");
        Files.createDirectories(plots);
        chart(plots.resolve("01_edit_work.png"), "Edit distance: repeated work in plain recursion",
                "Equal-length strings; measured character comparisons; logarithmic vertical axis",
                "String length n (characters)", "Character comparisons (count, log10)", false, true,
                List.of(series(data,"recursive","disjoint","Recursive: disjoint",RED,false,s->s.key.n<=12,s->s.comparisons),
                        series(data,"recursive","random","Recursive: random",GOLD,false,s->s.key.n<=12,s->s.comparisons),
                        series(data,"recursive","identical","Recursive: identical",GREEN,false,s->s.key.n<=12,s->s.comparisons),
                        series(data,"table","disjoint","Table / two rows: every input",BLUE,true,s->s.key.n<=12,s->s.comparisons)));
        chart(plots.resolve("02_dp_time.png"), "Edit distance: large-input DP runtime",
                "Random strings over abcd; equal lengths; median of 5 measured invocations",
                "String length n (characters)", "Median elapsed time (ms)", false, false,
                List.of(series(data,"table","random","Full table",BLUE,false,s->s.key.n>=128,s->s.median),
                        series(data,"two_rows","random","Two rows",GREEN,false,s->s.key.n>=128,s->s.median)));
        chart(plots.resolve("03_search_work.png"), "String search: cost of repeated prefixes",
                "Pattern length m = n / 256; includes KMP LPS preprocessing; both axes logarithmic",
                "Text length n (characters, log10)", "Total character comparisons (count, log10)", true, true,
                searchSeries(data,s->s.comparisons+s.lps));
        chart(plots.resolve("04_search_time.png"), "String search: measured runtime",
                "Pattern length m = n / 256; median of 5 invocations; both axes logarithmic",
                "Text length n (characters, log10)", "Median elapsed time (ms, log10)", true, true,
                searchSeries(data,s->s.median));
    }

    private static List<Series> searchSeries(List<Summary> data, ToDoubleFunction<Summary> metric) {
        return List.of(series(data,"naive","late_mismatch","Naive: late mismatch",RED,false,s->true,metric),
                series(data,"kmp","late_mismatch","KMP: late mismatch",BLUE,false,s->true,metric),
                series(data,"naive","random","Naive: random",GOLD,true,s->true,metric),
                series(data,"kmp","random","KMP: random",GREEN,true,s->true,metric));
    }

    private static Series series(List<Summary> data, String algorithm, String input, String label, Color color,
                                 boolean dashed, Predicate<Summary> filter, ToDoubleFunction<Summary> metric) {
        List<Point> points = data.stream().filter(s->s.key.algorithm.equals(algorithm) && s.key.input.equals(input))
                .filter(filter).sorted(Comparator.comparingInt(s->s.key.n)).map(s->new Point(s.key.n,metric.applyAsDouble(s))).toList();
        return new Series(label,color,dashed,points);
    }

    private static List<Summary> summarize(Path csv) throws IOException {
        Map<Key,List<String[]>> groups = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(csv);
        for (String line : lines.subList(1,lines.size())) {
            if (line.isBlank()) continue;
            String[] c = line.split(",");
            Key key = new Key(c[0],c[1],Integer.parseInt(c[2]),Integer.parseInt(c[3]));
            groups.computeIfAbsent(key,k->new ArrayList<>()).add(c);
        }
        List<Summary> summaries = new ArrayList<>();
        for (Map.Entry<Key,List<String[]>> entry : groups.entrySet()) {
            List<String[]> runs = entry.getValue();
            if (runs.size()!=5) throw new IOException("Expected 5 runs for " + entry.getKey());
            String[] c = runs.get(0);
            for (String[] run : runs) for (int i=6;i<c.length;i++) {
                if (!run[i].equals(c[i])) throw new IOException("Non-reproducible counters or inputs: " + entry.getKey());
            }
            double[] times = runs.stream().mapToDouble(r->Double.parseDouble(r[5])).sorted().toArray();
            summaries.add(new Summary(entry.getKey(),times[2],times[0],times[4],Long.parseLong(c[6]),
                    Long.parseLong(c[7]),Long.parseLong(c[8]),Long.parseLong(c[9]),Long.parseLong(c[10]),Integer.parseInt(c[11])));
        }
        return summaries;
    }

    private static void chart(Path file, String title, String subtitle, String xLabel, String yLabel,
                              boolean logX, boolean logY, List<Series> series) throws IOException {
        int width=1200,height=680,left=140,right=1130,top=105,bottom=480;
        List<Point> points=series.stream().flatMap(s->s.points.stream()).toList();
        double xMin=points.stream().mapToDouble(p->transform(p.x,logX)).min().orElseThrow();
        double xMax=points.stream().mapToDouble(p->transform(p.x,logX)).max().orElseThrow();
        double yMin=logY?Math.floor(points.stream().mapToDouble(p->transform(p.y,true)).min().orElseThrow()):0;
        double yMax=points.stream().mapToDouble(p->transform(p.y,logY)).max().orElseThrow();
        yMax=logY?Math.ceil(yMax):yMax*1.12;
        if(yMax==yMin)yMax=yMin+1;
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);g.fillRect(0,0,width,height);
        g.setColor(new Color(25,40,60));g.setFont(new Font("SansSerif",Font.BOLD,28));g.drawString(title,52,43);
        g.setColor(new Color(85,96,112));g.setFont(new Font("SansSerif",Font.PLAIN,18));g.drawString(subtitle,52,73);
        g.setFont(new Font("SansSerif",Font.PLAIN,17));
        int ticks=logY?(int)(yMax-yMin):5;
        for(int t=0;t<=ticks;t++) {
            double v=yMin+(yMax-yMin)*t/ticks;
            int py=bottom-(int)Math.round((v-yMin)/(yMax-yMin)*(bottom-top));
            g.setColor(new Color(225,231,238));g.setStroke(new BasicStroke(1));g.drawLine(left,py,right,py);
            String label=logY?"1e"+(int)v:String.format(Locale.ROOT,"%.2f",v);
            g.setColor(new Color(65,75,90));g.drawString(label,left-14-g.getFontMetrics().stringWidth(label),py+6);
        }
        for(double x:points.stream().mapToDouble(p->p.x).distinct().sorted().toArray()) {
            int px=left+(int)Math.round((transform(x,logX)-xMin)/(xMax-xMin)*(right-left));
            g.setColor(new Color(237,240,244));g.drawLine(px,top,px,bottom);
            g.setColor(new Color(65,75,90));center(g,String.format(Locale.ROOT,"%,.0f",x),px,bottom+28);
        }
        g.setColor(new Color(70,80,95));g.drawLine(left,top,left,bottom);g.drawLine(left,bottom,right,bottom);
        g.setFont(new Font("SansSerif",Font.PLAIN,20));center(g,xLabel,(left+right)/2,540);
        AffineTransform original=g.getTransform();g.rotate(-Math.PI/2);center(g,yLabel,-(top+bottom)/2,36);g.setTransform(original);
        for(Series s:series) {
            g.setColor(s.color);g.setStroke(s.dashed?new BasicStroke(3,0,0,10,new float[]{10,7},0):new BasicStroke(3));
            int lastX=0,lastY=0;boolean first=true;
            for(Point p:s.points) {
                int px=left+(int)Math.round((transform(p.x,logX)-xMin)/(xMax-xMin)*(right-left));
                int py=bottom-(int)Math.round((transform(p.y,logY)-yMin)/(yMax-yMin)*(bottom-top));
                if(!first)g.drawLine(lastX,lastY,px,py);
                g.fillOval(px-5,py-5,10,10);lastX=px;lastY=py;first=false;
            }
        }
        g.setFont(new Font("SansSerif",Font.PLAIN,19));
        for(int i=0;i<series.size();i++) {
            Series s=series.get(i);int x=140+(i%2)*490,y=590+(i/2)*34;
            g.setColor(s.color);g.setStroke(s.dashed?new BasicStroke(3,0,0,10,new float[]{10,7},0):new BasicStroke(3));
            g.drawLine(x,y-6,x+36,y-6);g.fillOval(x+13,y-11,10,10);
            g.setColor(new Color(40,50,65));g.drawString(s.label,x+48,y);
        }
        g.dispose();
        if(!ImageIO.write(image,"png",file.toFile()))throw new IOException("No PNG writer");
    }

    private static double transform(double value, boolean log) {
        if(log && value<=0)throw new IllegalArgumentException("Logarithmic data must be positive");
        return log?Math.log10(value):value;
    }
    private static void center(Graphics2D g,String text,int x,int y) { g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y); }
}
