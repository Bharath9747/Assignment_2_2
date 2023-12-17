package Java_Application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class InterviewDetails {
    private Date interviewDate;
    private LocalTime interviewTime;
    private String teamName;
    private String panelName;
    private String interviewRound;

}
