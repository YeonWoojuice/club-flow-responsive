package com.clubflow.backend.application;

import com.clubflow.backend.common.ConflictException;
import com.clubflow.backend.generation.Generation;
import com.clubflow.backend.person.Person;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ApplicationTest {

    @Test
    void 결과_메일_발송_전에는_합격과_불합격을_서로_정정할_수_있다() {
        Application application = Application.createManual(
                mock(Generation.class),
                mock(Person.class)
        );
        application.changeStatus(ApplicationStatus.ACCEPTED);

        application.changeStatus(ApplicationStatus.REJECTED);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void 같은_최종_상태를_다시_요청하면_멱등하게_처리한다() {
        Application application = Application.createManual(
                mock(Generation.class),
                mock(Person.class)
        );
        application.changeStatus(ApplicationStatus.ACCEPTED);

        assertThatCode(() -> application.changeStatus(ApplicationStatus.ACCEPTED))
                .doesNotThrowAnyException();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void 결과를_검토중으로_되돌릴_수_없다() {
        Application application = Application.createManual(mock(Generation.class), mock(Person.class));
        application.changeStatus(ApplicationStatus.ACCEPTED);

        assertThatThrownBy(() -> application.changeStatus(ApplicationStatus.REVIEWING))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("반대 결과로만 정정");
    }

    @Test
    void 취소된_지원서는_상태를_바꿀_수_없다() {
        Application application = Application.createManual(mock(Generation.class), mock(Person.class));
        application.changeStatus(ApplicationStatus.CANCELED);

        assertThatThrownBy(() -> application.changeStatus(ApplicationStatus.REVIEWING))
                .isInstanceOf(ConflictException.class)
                .hasMessage("취소된 지원 상태는 변경할 수 없습니다.");
    }
}
