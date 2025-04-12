package org.springframework.samples.petclinic.visits.model;

import org.junit.jupiter.api.Test;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;

class VisitBuilderTest {

    @Test
    void testVisitBuilder() {
        Integer id = 10;
        int petId = 111;
        String description = "Test Desc";
        Date date = new Date();

        // Sử dụng builder để tạo đối tượng Visit
        Visit visit = Visit.VisitBuilder.aVisit()
            .id(id)
            .petId(petId)
            .description(description) // Gọi các phương thức builder bị miss coverage
            .date(date)             // Gọi các phương thức builder bị miss coverage
            .build();

        // Kiểm tra các giá trị đã được set đúng
        assertThat(visit.getId()).isEqualTo(id);
        assertThat(visit.getPetId()).isEqualTo(petId);
        assertThat(visit.getDescription()).isEqualTo(description);
        assertThat(visit.getDate()).isEqualTo(date);
    }
}