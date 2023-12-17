package Java_Application;



import Java_Application.service.impl.CandidateServiceImpl;

import java.io.IOException;
import java.util.Map;

import static Java_Application.repo.CandidateRepo.canditateDetailsHashMap;


public class MainRunner {

    public static void main(String[] args) throws Exception {
        CandidateServiceImpl candidateService = new CandidateServiceImpl();

        String excelFileName = "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\data\\FinalAccolite.xlsx";
        String pdfFile = "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\pdf\\MonthWiseReport.pdf";
        String pdfFolder = "C:\\Users\\bharath.m\\IdeaProjects\\Assignment_2\\src\\main\\resources\\pdf\\Monthwise";
        candidateService.readDataFromExcel(excelFileName);

        candidateService.createConnection();
        candidateService.addRecords();
//        candidateService.maxNumberOfInterviews();
//        candidateService.minNumberOfInterviews();
//        candidateService.topPanels();
//        candidateService.topSkillsByMonthAndView();
//        candidateService.topSkillsByTime();
//        candidateService.createMonthChart();
//        candidateService.createMonthWiseCharts();
//        candidateService.addAllImagesToPdfMonthWise(pdfFile);
//        candidateService.addAllImagesToPdfMonth(pdfFolder);
    }
}