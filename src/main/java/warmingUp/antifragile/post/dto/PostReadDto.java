package warmingUp.antifragile.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import warmingUp.antifragile.comment.dto.CommentSendDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostReadDto {
    private String modelName;
    private String description;
    private String title;
    private String nickname;
    private LocalDateTime updatedAt;
    private Integer carAge;
    private Integer useYear;
    private Integer useMonth;
    private String purpose;
    private Long mgp;
    private Long safe;
    private Long space;
    private Long design;
    private Long fun;
    private String contents;
    private Long commentCount;
    private List<CommentSendDto> commentList;

}
