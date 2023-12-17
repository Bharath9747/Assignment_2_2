package Java_Application.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface CandidateServices {
    void readDataFromExcel(String excelFileName) ;

    void createConnection();
    void addRecords();

    void maxNumberOfInterviews();
    void minNumberOfInterviews();

    void topPanels();

    void topSkillsByMonthAndView();

    void topSkillsByTime();

    void createMonthChart();

    void createMonthWiseCharts();

    void addAllImagesToPdfMonthWise(String filename);

    void addAllImagesToPdfMonth(String pdfFolder);
}
