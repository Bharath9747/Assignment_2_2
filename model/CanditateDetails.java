package Java_Application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class CanditateDetails {

    private int Id;
    private String Name;
    private String Skill;
    private InterviewDetails interviewDetailsList;
    private WorkLocation workLocation;

}
