package Java_Application.service.impl;


import Java_Application.model.CanditateDetails;
import Java_Application.model.InterviewDetails;
import Java_Application.model.WorkLocation;
import Java_Application.service.CandidateServices;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.border.Border;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TextAlignment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static Java_Application.repo.CandidateRepo.canditateDetailsHashMap;

public class CandidateServiceImpl implements CandidateServices {
    AtomicInteger atomicInteger = new AtomicInteger(0001);
    Connection connection = null;
    Map<String,Long> monthCount = null;
    Map<String,Map<String,Long>> skillCountByMonth =null;
    Map<String,Map<String,Long>> workLocationByMonth = null;
    List<Map<String, Long>> interviewRoundCountList = new ArrayList<>();
    List<Map<String,Long>> workLocationCountList = new ArrayList<>();

    List<Map<String, Long>> preferedLocationCountLiist = new ArrayList<>();
    List<Map<String,Long>> skillCountList = new ArrayList<>();
    List<Map<String, Long>> teamCountList = new ArrayList<>();

    @Override
    public void readDataFromExcel(String excelFileName)  {
        try {
            FileInputStream inputStream = new FileInputStream(excelFileName);
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            for (int i = 1; i < rowCount; i++) {
                Row row = sheet.getRow(i);
                String stringdate = String.valueOf(row.getCell(0));
                    Date date = parseDate(stringdate);

                    Cell cell = row.getCell(6);

                        String name = String.valueOf(row.getCell(9)).toUpperCase().trim();
                        String skill = String.valueOf(row.getCell(5)).toUpperCase().trim();
                        double excelTimeValue = cell.getNumericCellValue();
                        LocalTime time = parseTime(excelTimeValue);
                        String teamName = String.valueOf(row.getCell(2)).toUpperCase().trim();
                        String panelName = String.valueOf(row.getCell(3)).toUpperCase().trim();
                        String interviewRound = String.valueOf(row.getCell(4));
                        String preferredLocation = String.valueOf(row.getCell(8)).toUpperCase().trim();
                        String workLocation = String.valueOf(row.getCell(7)).toUpperCase().trim();
                            int id = atomicInteger.getAndIncrement();
                            WorkLocation workLocation1 = new WorkLocation(preferredLocation, workLocation);
                            InterviewDetails interviewDetails = new InterviewDetails(date, time, teamName, panelName, interviewRound);
                            CanditateDetails canditateDetails = new CanditateDetails(id, name, skill, interviewDetails, workLocation1);
                            canditateDetailsHashMap.put(id, canditateDetails);


            }
            workbook.close();
            inputStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void createConnection() {
        try {

           Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Accolite","root","1234");
             System.out.println("Connection Established");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void addRecords() {
        try {
            Statement truncateStatement = connection.createStatement();
            String truncateQuery = "Truncate table Interview_Status";
            truncateStatement.executeUpdate(truncateQuery);
            canditateDetailsHashMap.entrySet().stream().forEach(
                    entry->
                    {
                        String insertQuery = "INSERT INTO Interview_Status (Id, Name, Skill, InterviewDate, InterviewTime, TeamName, PanelName, InterviewRound, PreferredLocation, WorkLocation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        PreparedStatement insertStatement = null;
                        try {
                            insertStatement = connection.prepareStatement(insertQuery);
                            CanditateDetails candidate = entry.getValue();
                            insertStatement.setInt(1, entry.getKey());
                            insertStatement.setString(2, candidate.getName());
                            insertStatement.setString(3, candidate.getSkill());
                            insertStatement.setDate(4, new java.sql.Date(candidate.getInterviewDetailsList().getInterviewDate().getTime()));
                            insertStatement.setTime(5, Time.valueOf(candidate.getInterviewDetailsList().getInterviewTime()));
                            insertStatement.setString(6, candidate.getInterviewDetailsList().getTeamName());
                            insertStatement.setString(7, candidate.getInterviewDetailsList().getPanelName());
                            insertStatement.setString(8, candidate.getInterviewDetailsList().getInterviewRound());
                            insertStatement.setString(9, candidate.getWorkLocation().getPreferredLocation());
                            insertStatement.setString(10, candidate.getWorkLocation().getWorkLocation());



                            insertStatement.executeUpdate();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }

            );
            System.out.println("Insertion Done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void maxNumberOfInterviews() {
        String sqlQuery = "select TeamName , count(*) as InterviewCount from Interview_Status where month(InterviewDate) in (10,11)\n" +
                "group by TeamName order by InterviewCount desc limit 1;";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {
                String teamName = resultSet.getString("TeamName");
                int interviewCount = resultSet.getInt("InterviewCount");

                System.out.println("Team with the most interviews: " + teamName);
                System.out.println("Interview count: " + interviewCount);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    public void minNumberOfInterviews() {
        String sqlQuery = "select TeamName , count(*) as InterviewCount from Interview_Status where month(InterviewDate) in (10,11)\n" +
                "group by TeamName order by InterviewCount  limit 1;";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {
                String teamName = resultSet.getString("TeamName");
                int interviewCount = resultSet.getInt("InterviewCount");

                System.out.println("Team with the mininimum interviews: " + teamName);
                System.out.println("Interview count: " + interviewCount);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void topPanels() {
        List<InterviewDetails> interviewDetailsList = getAllInterviewDetails();
        Map<String,Long> panelCount = interviewDetailsList.stream().filter(data->isMonth(data.getInterviewDate())).collect(Collectors.groupingBy(InterviewDetails::getPanelName,Collectors.counting()));
        panelCount.entrySet().stream().sorted(Map.Entry.<String ,Long>comparingByValue().reversed()).limit(3).forEach(
                (x)-> System.out.println(x.getKey()+" "+x.getValue())
        );

    }

    @Override
    public void topSkillsByMonthAndView() {

        try {
            String sql = "create or replace view  TopSkills as select skill,count(*) as SkillCount from Interview_Status where Month(InterviewDate) in (10,11) group by Skill Order by skillcount desc limit 3;";
            Statement st = connection.createStatement();
            st.executeUpdate(sql);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        String sqlQuery = "select * from TopSkills";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                String teamName = resultSet.getString("Skill");
                int interviewCount = resultSet.getInt("SkillCount");

                System.out.println("Skill : " + teamName);
                System.out.println("Count: " + interviewCount);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void topSkillsByTime() {

        String sqlQuery = "select InterviewTime,count(*) as InterviewTimeCount from Interview_Status  group by InterviewTime Order by InterviewTimeCount desc limit 1;";
        String time="";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                time = resultSet.getString("InterviewTime");
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        System.out.println("Peak Time : "+time);

        String sqlQuery1 = "select skill,count(*) as SkillCount from Interview_Status where InterviewTime = '"+time+"' group by Skill Order by skillcount desc limit 3;";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery1);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                String teamName = resultSet.getString("Skill");
                int interviewCount = resultSet.getInt("SkillCount");

                System.out.println("Skill : " + teamName);
                System.out.println("Count: " + interviewCount);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void createMonthChart() {
        List<InterviewDetails> interviewDetailsList = getAllInterviewDetails();
        HashMap<Integer,String> months = new HashMap<>();
        months.put(10,"October");
        months.put(11,"November");
        months.put(12,"December");
        monthCount = interviewDetailsList.stream().collect(Collectors.groupingBy(interviewDetails -> months.get(interviewDetails.getInterviewDate().getMonth()+1),Collectors.counting()));
        DefaultPieDataset dataset = new DefaultPieDataset( );
        monthCount.entrySet().forEach(
                (x)->dataset.setValue(x.getKey(),x.getValue())
        );

        JFreeChart chart = ChartFactory.createPieChart(
                "Interviews Count Per Month",
                dataset,
                true,
                true,
                false);
        int width = 640;
        int height = 480;
        File pieChart = new File( "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\MonthCount.jpeg" );
        try {
            ChartUtilities.saveChartAsJPEG( pieChart , chart , width , height );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        skillCountByMonth = canditateDetailsHashMap.values().stream()
                .collect(Collectors.groupingBy(
                        canditateDetails -> months.get(canditateDetails.getInterviewDetailsList().getInterviewDate().getMonth()+1),
                        Collectors.groupingBy(CanditateDetails::getSkill,Collectors.counting())
                ));
        DefaultCategoryDataset dataset1 = new DefaultCategoryDataset( );
        dataset1.setValue(skillCountByMonth.get("October").get("JAVA"),"October","JAVA");
        dataset1.setValue(skillCountByMonth.get("October").get("ANGULAR"),"October","ANGULAR");
        dataset1.setValue(skillCountByMonth.get("October").get("PRODUCTION SUPPORT"),"October","PRODUCTION SUPPORT");
        dataset1.setValue(skillCountByMonth.get("November").get("JAVA"),"November","JAVA");
        dataset1.setValue(skillCountByMonth.get("November").get("ANGULAR"),"November","ANGULAR");
        dataset1.setValue(skillCountByMonth.get("November").get("PRODUCTION SUPPORT"),"November","PRODUCTION SUPPORT");
        dataset1.setValue(skillCountByMonth.get("December").get("JAVA"),"December","JAVA");
        dataset1.setValue(skillCountByMonth.get("December").get("ANGULAR"),"December","ANGULAR");
        dataset1.setValue(skillCountByMonth.get("December").get("PRODUCTION SUPPORT"),"December","PRODUCTION SUPPORT");


        JFreeChart barChart = ChartFactory.createBarChart(
                "Skill  STATIStICS",
                "Skills", "No of Employees",
                dataset1, PlotOrientation.VERTICAL,
                true, true, false);
        File BarChart = new File( "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\SkillByMonth.jpeg" );
        try {
            ChartUtilities.saveChartAsJPEG( BarChart , barChart , width , 900 );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DefaultCategoryDataset dataset2 = new DefaultCategoryDataset( );
        workLocationByMonth = canditateDetailsHashMap.values().stream()
                .collect(Collectors.groupingBy(
                        canditateDetails -> months.get(canditateDetails.getInterviewDetailsList().getInterviewDate().getMonth()+1),
                        Collectors.groupingBy(canditateDetails -> canditateDetails.getWorkLocation().getWorkLocation(),Collectors.counting())
                ));
        dataset2.setValue(workLocationByMonth.get("October").get("BANGALORE"),"October","BANGALORE");
        dataset2.setValue(workLocationByMonth.get("October").get("HYDERABAD"),"October","HYDERABAD");
        dataset2.setValue(workLocationByMonth.get("October").get("CHENNAI"),"October","CHENNAI");
        dataset2.setValue(workLocationByMonth.get("November").get("BANGALORE"),"November","BANGALORE");
        dataset2.setValue(workLocationByMonth.get("November").get("HYDERABAD"),"November","HYDERABAD");
        dataset2.setValue(workLocationByMonth.get("November").get("CHENNAI"),"November","CHENNAI");
        dataset2.setValue(workLocationByMonth.get("December").get("BANGALORE"),"December","BANGALORE");
        dataset2.setValue(workLocationByMonth.get("December").get("HYDERABAD"),"December","HYDERABAD");
        dataset2.setValue(workLocationByMonth.get("December").get("CHENNAI"),"December","CHENNAI");

        JFreeChart barChart2 = ChartFactory.createBarChart(
                "Work Location  STATIStICS",
                "Location", "No of Employees",
                dataset2, PlotOrientation.VERTICAL,
                true, true, false);
        File BarChart2 = new File( "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\WorkLocation.jpeg" );
        try {
            ChartUtilities.saveChartAsJPEG( BarChart2 , barChart2 , width , 900 );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createMonthWiseCharts() {
        for (int i=10;i<=12;i++) {
            List<InterviewDetails> interviewDetailsList = getAllInterviewDetails();

            int finalI = i-1;
            Map<String, Long> interviewRoundCount = interviewDetailsList.stream().filter(interviewDetails -> interviewDetails.getInterviewDate().getMonth()== finalI).collect(Collectors.groupingBy(interviewDetails -> interviewDetails.getInterviewRound(), Collectors.counting()));

            Map<String, Long> workLocationCount = canditateDetailsHashMap.values().stream().filter(canditateDetails -> canditateDetails.getInterviewDetailsList().getInterviewDate().getMonth()==finalI).map(CanditateDetails::getWorkLocation).map(WorkLocation::getWorkLocation).collect(Collectors.toList()).stream().collect(Collectors.groupingBy(workLocation->workLocation,Collectors.counting()));
            Map<String, Long> preferedLocationCount =canditateDetailsHashMap.values().stream().filter(canditateDetails -> canditateDetails.getInterviewDetailsList().getInterviewDate().getMonth()==finalI).map(CanditateDetails::getWorkLocation).map(WorkLocation::getPreferredLocation).collect(Collectors.toList()).stream().collect(Collectors.groupingBy(preferedLocation->preferedLocation,Collectors.counting()));
            Map<String, Long> skillCount =canditateDetailsHashMap.values().stream().filter(canditateDetails -> canditateDetails.getInterviewDetailsList().getInterviewDate().getMonth()==finalI).map(CanditateDetails::getSkill).collect(Collectors.toList()).stream().collect(Collectors.groupingBy(skill->skill,Collectors.counting()));
            Map<String, Long> teamCount = interviewDetailsList.stream().filter(interviewDetails -> interviewDetails.getInterviewDate().getMonth()== finalI).collect(Collectors.groupingBy(interviewDetails -> interviewDetails.getTeamName(), Collectors.counting()));

            DefaultPieDataset dataset = new DefaultPieDataset( );
            interviewRoundCount.entrySet().forEach(
                    (x)->dataset.setValue(x.getKey(),x.getValue())
            );

            JFreeChart chart = ChartFactory.createPieChart(
                    "Interviews Rounds Count",
                    dataset,
                    true,
                    true,
                    false);
            int width = 640;
            int height = 480;
            File pieChart = new File( "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\InterviewRoundCount"+i+".jpeg" );
            try {
                ChartUtilities.saveChartAsJPEG( pieChart , chart , width , height );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            interviewRoundCountList.add(interviewRoundCount);

            List<String > locations = new ArrayList<>(Arrays.asList("CHENNAI","BANGALORE","HYDERABAD"));
            DefaultPieDataset dataset1 = new DefaultPieDataset( );
            final int[] s = {0};
            workLocationCount.entrySet().forEach(
                    (x)->   {if(locations.contains(x.getKey()))
                            dataset1.setValue(x.getKey(),x.getValue());
                        else
                            s[0] = s[0] +1;
                    }
            );
            dataset1.setValue("Others",s[0]);

            JFreeChart chart1 = ChartFactory.createPieChart(
                    "Work Location Count",
                    dataset1,
                    true,
                    true,
                    false);
            File pieChart1 = new File( "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\WorkLocation"+i+".jpeg" );
            try {
                ChartUtilities.saveChartAsJPEG( pieChart1 , chart1 , width , height );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            workLocationCountList.add(workLocationCount);
            DefaultPieDataset dataset2 = new DefaultPieDataset( );
            s[0]=0;
            preferedLocationCount.entrySet().forEach(
                    (x)->   {if(locations.contains(x.getKey()))
                        dataset2.setValue(x.getKey(),x.getValue());
                    else
                        s[0] = s[0] +1;
                    }
            );
            dataset2.setValue("Others",s[0]);
            preferedLocationCountLiist.add(preferedLocationCount);

            JFreeChart chart2 = ChartFactory.createPieChart(
                    "Preferred Location Count",
                    dataset2,
                    true,
                    true,
                    false);
            File pieChart2 = new File( "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\PreferredLocation"+i+".jpeg" );
            try {
                ChartUtilities.saveChartAsJPEG( pieChart2 , chart2 , width , height );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<String > skills = new ArrayList<>(Arrays.asList("JAVA","ANGULAR","QA"));
            DefaultPieDataset dataset3 = new DefaultPieDataset( );
            s[0]=0;
            skillCount.entrySet().forEach(
                    (x)->   {if(skills.contains(x.getKey()))
                        dataset3.setValue(x.getKey(),x.getValue());
                    else
                        s[0] = s[0] +1;
                    }
            );
            dataset3.setValue("Others",s[0]);
            skillCountList.add(skillCount);
            JFreeChart chart3 = ChartFactory.createPieChart(
                    "Skills Count",
                    dataset3,
                    true,
                    true,
                    false);
            File pieChart3 = new File( "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\Skill"+i+".jpeg" );
            try {
                ChartUtilities.saveChartAsJPEG( pieChart3 , chart3 , width , height );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            DefaultPieDataset dataset4 = new DefaultPieDataset( );
            s[0]=0;
            teamCount.entrySet().forEach(
                    (x)->   {if(x.getKey().equals("BENCH"))
                        dataset4.setValue(x.getKey(),x.getValue());
                    else
                        s[0] = s[0] +1;
                    }
            );
            dataset4.setValue("Others",s[0]);
            teamCountList.add(teamCount);
            JFreeChart chart4 = ChartFactory.createPieChart(
                    "Team Count",
                    dataset4,
                    true,
                    true,
                    false);
            File pieChart4 = new File( "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\TeamCount"+i+".jpeg" );
            try {
                ChartUtilities.saveChartAsJPEG( pieChart4 , chart4 , width , height );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    public void addAllImagesToPdfMonthWise(String fileName) {
        try {
            File file = new File(fileName);
            file.createNewFile();
            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument document = new PdfDocument(writer);
            document.addNewPage();
            Document doc = new Document(document);

            document.setDefaultPageSize(PageSize.A4);
            doc.add(new Paragraph()
                            .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(50)
                    .setBold()
                    .add(new Text("Month Wise Report"))
            );
            doc.add(
                    new Paragraph()
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setFontSize(30)
                            .add("By Bharath")
            );
            doc.add(
                new Image(ImageDataFactory.create("C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\MonthCount.jpeg")).setTextAlignment(TextAlignment.CENTER)
            );
            float colwid1[] = {260, 260};
            com.itextpdf.layout.element.Table t1 = new com.itextpdf.layout.element.Table(colwid1).setFontSize(15f);
            t1.addCell(new com.itextpdf.layout.element.Cell().add("Month"));
            t1.addCell(new com.itextpdf.layout.element.Cell().add("Interview Count"));
            monthCount.entrySet().forEach(
                    (x)-> {
                        t1.addCell(new com.itextpdf.layout.element.Cell().add(x.getKey()));
                        t1.addCell(new com.itextpdf.layout.element.Cell().add(x.getValue()+""));
                    }
            );
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(t1);
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("\n"));
            doc.add(
                    new Image(ImageDataFactory.create("C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\SkillByMonth.jpeg")).setTextAlignment(TextAlignment.CENTER)
            );
            Map<String, Map<String, Long>> sortedSkillCountByMonth = skillCountByMonth.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().entrySet().stream()
                                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue,
                                            (e1, e2) -> e1,
                                            LinkedHashMap::new
                                    )),
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));

            com.itextpdf.layout.element.Table t2 = new com.itextpdf.layout.element.Table(colwid1).setFontSize(15f);
            t2.addCell(new com.itextpdf.layout.element.Cell().add("Skill Count"));
            t2.addCell(new com.itextpdf.layout.element.Cell().add("Skill Count"));
            doc.add(new Paragraph("\n"));
            sortedSkillCountByMonth.entrySet().forEach(
                    (x)->{
                        t2.addCell(new com.itextpdf.layout.element.Cell().add("Month "+x.getKey()).setBold());
                        t2.addCell(new com.itextpdf.layout.element.Cell().add(""));
                        x.getValue().entrySet().forEach(
                                (y)->{
                                    t2.addCell(new com.itextpdf.layout.element.Cell().add(y.getKey()+" "+y.getValue()));

                                }

                        );
                        t2.addCell(new com.itextpdf.layout.element.Cell().add(""));
                    }
            );
            t2.addCell(new com.itextpdf.layout.element.Cell().add(""));
            doc.add(t2);
            doc.add(new Paragraph()
                    .setRotationAngle(Math.PI/4)
                    .setFontSize(30)
                    .setBold()
                    .add(new Text("Month Wise Report"))
            );
            doc.add(
                    new Image(ImageDataFactory.create("C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\WorkLocation.jpeg")).setTextAlignment(TextAlignment.CENTER)
            );
            Map<String, Map<String, Long>> sortedworkLocationByMonth = workLocationByMonth.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().entrySet().stream()
                                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue,
                                            (e1, e2) -> e1,
                                            LinkedHashMap::new
                                    )),
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
            com.itextpdf.layout.element.Table t3 = new com.itextpdf.layout.element.Table(colwid1).setFontSize(15f);
            t3.addCell(new com.itextpdf.layout.element.Cell().add("Location Count"));
            t3.addCell(new com.itextpdf.layout.element.Cell().add("Location Count"));
            sortedworkLocationByMonth.entrySet().forEach(
                    (x)->{
                        t3.addCell(new com.itextpdf.layout.element.Cell().add("Month "+x.getKey()).setBold());
                        t3.addCell(new com.itextpdf.layout.element.Cell().add(""));
                        x.getValue().entrySet().forEach(
                                (y)->{
                                    t3.addCell(new com.itextpdf.layout.element.Cell().add(y.getKey()+" "+y.getValue()));
                                }

                        );
                        t3.addCell(new com.itextpdf.layout.element.Cell().add(""));
                    }
            );

            doc.add(new Paragraph("\n"));
            t3.addCell(new com.itextpdf.layout.element.Cell().add(""));
            doc.add(t3);
            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAllImagesToPdfMonth(String pdfFolder) {
        List<String> months = new ArrayList<>(Arrays.asList("October","November","December"));
        try {
            for (int i=0;i<=2;i++) {
                String fileName = pdfFolder +"\\"+ months.get(i)+".pdf";
                File file = new File(fileName);
                file.createNewFile();
                PdfWriter writer = new PdfWriter(fileName);
                PdfDocument document = new PdfDocument(writer);
                document.addNewPage();
                Document doc = new Document(document);

                document.setDefaultPageSize(PageSize.A4);

                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(50)
                        .setBold()
                        .add(new Text(months.get(i)+" Report"))
                );
                doc.add(
                        new Paragraph()
                                .setTextAlignment(TextAlignment.RIGHT)
                                .setFontSize(30)
                                .add("By Bharath")
                );
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(
                        new Image(ImageDataFactory.create("C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\InterviewRoundCount"+(i+10)+".jpeg")).setTextAlignment(TextAlignment.CENTER)
                );

                Map<String,Long> temp   = interviewRoundCountList.get(i).entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1,e2)->e1,
                        LinkedHashMap::new
                )) ;
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));doc.add(new Paragraph("\n"));
                float colwid1[] = {260, 260};
                com.itextpdf.layout.element.Table t1 = new com.itextpdf.layout.element.Table(colwid1).setFontSize(15f);
                t1.addCell(new com.itextpdf.layout.element.Cell().add("Interview Round").setBold());
                t1.addCell(new com.itextpdf.layout.element.Cell().add("Count").setBold());
                temp.entrySet().forEach(
                        (x)-> {
                            t1.addCell(new com.itextpdf.layout.element.Cell().add(x.getKey()));
                            t1.addCell(new com.itextpdf.layout.element.Cell().add(x.getValue()+""));
                        }
                );
                doc.add(t1);
                doc.add(new Paragraph("\n"));
                temp   = workLocationCountList.get(i).entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1,e2)->e1,
                        LinkedHashMap::new
                )) ;

                doc.add(
                        new Image(ImageDataFactory.create("C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\WorkLocation"+(i+10)+".jpeg")).setTextAlignment(TextAlignment.CENTER)
                );
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                com.itextpdf.layout.element.Table t2 = new com.itextpdf.layout.element.Table(colwid1).setFontSize(15f);
                t2.addCell(new com.itextpdf.layout.element.Cell().add("Location").setBold());
                t2.addCell(new com.itextpdf.layout.element.Cell().add("Count").setBold());
                temp.entrySet().forEach(
                        (x)-> {
                            t2.addCell(new com.itextpdf.layout.element.Cell().add(x.getKey()));
                            t2.addCell(new com.itextpdf.layout.element.Cell().add(x.getValue()+""));
                        }
                );
                doc.add(t2);
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                temp   = preferedLocationCountLiist.get(i).entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1,e2)->e1,
                        LinkedHashMap::new
                )) ;
                doc.add(
                        new Image(ImageDataFactory.create("C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\PreferredLocation"+(i+10)+".jpeg")).setTextAlignment(TextAlignment.CENTER)
                );
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                com.itextpdf.layout.element.Table t3 = new com.itextpdf.layout.element.Table(colwid1).setFontSize(15f);
                t3.addCell(new com.itextpdf.layout.element.Cell().add("Location").setBold());
                t3.addCell(new com.itextpdf.layout.element.Cell().add("Count").setBold());
                temp.entrySet().forEach(
                        (x)-> {
                            t3.addCell(new com.itextpdf.layout.element.Cell().add(x.getKey()));
                            t3.addCell(new com.itextpdf.layout.element.Cell().add(x.getValue()+""));
                        }
                );
                doc.add(t3);
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                temp   = skillCountList.get(i).entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1,e2)->e1,
                        LinkedHashMap::new
                )) ;
                doc.add(
                        new Image(ImageDataFactory.create("C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\Skill"+(i+10)+".jpeg")).setTextAlignment(TextAlignment.CENTER)
                );
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                com.itextpdf.layout.element.Table t4 = new com.itextpdf.layout.element.Table(colwid1).setFontSize(15f);
                t4.addCell(new com.itextpdf.layout.element.Cell().add("Skill").setBold());
                t4.addCell(new com.itextpdf.layout.element.Cell().add("Count").setBold());
                temp.entrySet().forEach(
                        (x)-> {
                            t4.addCell(new com.itextpdf.layout.element.Cell().add(x.getKey()));
                            t4.addCell(new com.itextpdf.layout.element.Cell().add(x.getValue()+""));
                        }
                );
                doc.add(t4);
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                temp   = teamCountList.get(i).entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1,e2)->e1,
                        LinkedHashMap::new
                )) ;
                doc.add(
                        new Image(ImageDataFactory.create("C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\img\\TeamCount"+(i+10)+".jpeg")).setTextAlignment(TextAlignment.CENTER)
                );
                doc.add(new Paragraph("\n"));
                doc.add(new Paragraph("\n"));
                com.itextpdf.layout.element.Table t5 = new com.itextpdf.layout.element.Table(colwid1).setFontSize(15f);
                t5.addCell(new com.itextpdf.layout.element.Cell().add("Team").setBold());
                t5.addCell(new com.itextpdf.layout.element.Cell().add("Count").setBold());
                temp.entrySet().forEach(
                        (x)-> {
                            t5.addCell(new com.itextpdf.layout.element.Cell().add(x.getKey()));
                            t5.addCell(new com.itextpdf.layout.element.Cell().add(x.getValue()+""));
                        }
                );
                doc.add(t5);
                doc.close();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean isMonth(Date interviewDate) {
        int month = interviewDate.getMonth()+1;
        if(month==10 || month==11)
            return true;
        return false;
    }


    private List<InterviewDetails> getAllInterviewDetails() {
        return canditateDetailsHashMap.values().stream().map(CanditateDetails::getInterviewDetailsList).collect(Collectors.toList());
    }




    private LocalTime parseTime(double s) throws ParseException {
        ;
        long javaTimeValue = Math.round((s-25569)*86400*1000);

        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaTimeValue), ZoneId.of("Asia/Kolkata"));
        LocalTime localTime = localDateTime.toLocalTime().minusHours(5).minusMinutes(21).minusSeconds(10);
        return localTime;
    }

    private Date parseDate(String s) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy");
        return sdf.parse(s);
    }



}
