package org.springframework.samples.petclinic.vets.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List; 

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Vet} based on actual source code.
 */
class VetTest {

    private Vet vet;
    private Specialty radiology;
    private Specialty surgery;

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng Vet mới trước mỗi test method
        vet = new Vet();
        vet.setId(1);
        vet.setFirstName("James");
        vet.setLastName("Carter");

        // Khởi tạo các đối tượng Specialty
        radiology = new Specialty();
        radiology.setName("radiology");

        surgery = new Specialty();
        surgery.setName("surgery");
    }

    @Test
    void testGetNrOfSpecialtiesInitial() {
        // Kiểm tra số lượng specialties ban đầu (phải là 0)
        assertEquals(0, vet.getNrOfSpecialties(), "Initial number of specialties should be 0");
        // Kiểm tra list trả về từ getSpecialties() có rỗng không
        assertTrue(vet.getSpecialties().isEmpty(), "Specialties list from getSpecialties() should be initially empty");
    }

    @Test
    void testAddSpecialty() {
        // Thêm specialty đầu tiên
        vet.addSpecialty(radiology);
        assertEquals(1, vet.getNrOfSpecialties(), "Number of specialties should be 1 after adding one");

        // Lấy danh sách specialties (đã sắp xếp và không thể sửa đổi)
        List<Specialty> specialtiesList = vet.getSpecialties();
        assertEquals(1, specialtiesList.size(), "Returned list should have 1 specialty");
        // Kiểm tra xem specialty có trong list không (dựa vào object hoặc name)
        assertTrue(specialtiesList.stream().anyMatch(s -> s.getName().equals("radiology")), "Specialties list should contain radiology");


        // Thêm specialty thứ hai
        vet.addSpecialty(surgery);
        assertEquals(2, vet.getNrOfSpecialties(), "Number of specialties should be 2 after adding two");

        specialtiesList = vet.getSpecialties(); // Lấy lại list mới
        assertEquals(2, specialtiesList.size(), "Returned list should have 2 specialties");
        assertTrue(specialtiesList.stream().anyMatch(s -> s.getName().equals("surgery")), "Specialties list should contain surgery");
         // Kiểm tra thứ tự sắp xếp theo tên (nếu cần)
        assertEquals("radiology", specialtiesList.get(0).getName(), "First specialty in sorted list should be radiology");
        assertEquals("surgery", specialtiesList.get(1).getName(), "Second specialty in sorted list should be surgery");
    }

    @Test
    void testAddDuplicateSpecialty() {
        // Thêm một specialty
        vet.addSpecialty(radiology);
        assertEquals(1, vet.getNrOfSpecialties(), "Number of specialties should be 1");

        // Thêm lại chính specialty đó (không nên tăng số lượng vì dùng Set bên trong)
        vet.addSpecialty(radiology);
        assertEquals(1, vet.getNrOfSpecialties(), "Number of specialties should still be 1 after adding duplicate");

        List<Specialty> specialtiesList = vet.getSpecialties();
        assertEquals(1, specialtiesList.size(), "Returned list should still have 1 specialty after adding duplicate");
    }


    @Test
    void testGetSpecialtiesReturnsUnmodifiableList() {
        vet.addSpecialty(radiology);
        List<Specialty> specialties = vet.getSpecialties();
        assertNotNull(specialties, "getSpecialties() should not return null");

        // Kiểm tra xem list có phải là unmodifiable không
        assertThrows(UnsupportedOperationException.class, () -> {
            specialties.add(surgery); // Thử thêm vào list trả về -> sẽ lỗi
        }, "List returned by getSpecialties() should be unmodifiable");

        assertThrows(UnsupportedOperationException.class, () -> {
            specialties.remove(0); // Thử xóa khỏi list trả về -> sẽ lỗi
        }, "List returned by getSpecialties() should be unmodifiable");
    }

     @Test
    void testGettersAndSetters() {
        // Test các getter/setter cơ bản của Vet
        vet.setId(10);
        assertEquals(10, vet.getId());

        vet.setFirstName("Helen");
        assertEquals("Helen", vet.getFirstName());

        vet.setLastName("Leary");
        assertEquals("Leary", vet.getLastName());

        // Test getter/setter của Specialty (chỉ có name)
        Specialty spec = new Specialty();
        spec.setName("dentistry");
        assertEquals("dentistry", spec.getName());
        // Không thể test getId() nếu không có cách set ID hợp lệ từ bên ngoài
        // assertNull(spec.getId()); // ID có thể null ban đầu
    }
}