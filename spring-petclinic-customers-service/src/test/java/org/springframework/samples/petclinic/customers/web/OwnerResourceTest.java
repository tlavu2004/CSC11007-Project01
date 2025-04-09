package org.springframework.samples.petclinic.customers.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.samples.petclinic.customers.web.mapper.OwnerEntityMapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(SpringExtension.class)
@WebMvcTest(OwnerResource.class)
@ActiveProfiles("test")
class OwnerResourceTest {

    // ... (Autowired, MockBean, setUp giữ nguyên) ...
     @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper; // Để convert object thành JSON

    @MockBean
    OwnerRepository ownerRepository;

    @MockBean
    OwnerEntityMapper ownerEntityMapper;

    private Owner owner1;
    private Owner owner2;

    @BeforeEach
    void setUp() {
        owner1 = new Owner();
        // Cần setter cho ID nếu dùng trong test
        // owner1.setId(1);
        owner1.setFirstName("George");
        owner1.setLastName("Franklin");
        owner1.setAddress("110 W. Liberty St.");
        owner1.setCity("Madison");
        owner1.setTelephone("6085551023");

        owner2 = new Owner();
        // owner2.setId(2);
        owner2.setFirstName("Betty");
        owner2.setLastName("Davis");
        owner2.setAddress("638 Cardinal Ave.");
        owner2.setCity("Sun Prairie");
        owner2.setTelephone("6085551749");
    }


    @Test
    void testCreateOwnerSuccess() throws Exception {
        OwnerRequest request = new OwnerRequest("New", "Owner", "Address", "City", "111222333");
        Owner savedOwner = new Owner(); // Giả lập owner sau khi lưu
        // Giả lập ID được gán sau khi lưu
        // savedOwner.setId(3);
        savedOwner.setFirstName(request.firstName());
        // ... set các trường khác

        // Giả lập hành vi của mapper và repository
        given(ownerEntityMapper.map(any(Owner.class), eq(request))).willReturn(savedOwner); 
        given(ownerRepository.save(any(Owner.class))).willReturn(savedOwner);

        mvc.perform(post("/owners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("New"));
            // Thêm các kiểm tra khác nếu cần

        verify(ownerRepository).save(any(Owner.class));
        verify(ownerEntityMapper).map(any(Owner.class), eq(request));
    }

     @Test
    void testCreateOwnerBadRequest() throws Exception {
        // Gửi request thiếu trường @NotBlank
        OwnerRequest request = new OwnerRequest("", "Owner", "Address", "City", "111222333");

        mvc.perform(post("/owners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest()); // Spring Validation sẽ xử lý
    }


    @Test
    void testFindOwnerFound() throws Exception {
        given(ownerRepository.findById(1)).willReturn(Optional.of(owner1));

        mvc.perform(get("/owners/{ownerId}", 1).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.firstName").value("George"))
            .andExpect(jsonPath("$.lastName").value("Franklin"));
    }


    @Test
    void testFindOwnerNotFound() throws Exception {
        // Arrange
        given(ownerRepository.findById(99)).willReturn(Optional.empty());

        // Act & Assert
        mvc.perform(get("/owners/{ownerId}", 99).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

     @Test
    void testFindOwnerBadRequestInvalidId() throws Exception {
         // ID phải >= 1 do có @Min(1)
         mvc.perform(get("/owners/{ownerId}", 0).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testFindAllOwners() throws Exception {
        List<Owner> owners = Arrays.asList(owner1, owner2);
        given(ownerRepository.findAll()).willReturn(owners);

        mvc.perform(get("/owners").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].firstName").value("George"))
            .andExpect(jsonPath("$[1].firstName").value("Betty"));
    }

    @Test
    void testUpdateOwnerSuccess() throws Exception {
        int ownerId = 1;
        OwnerRequest request = new OwnerRequest("UpdatedGeorge", "UpdatedFranklin", "New Address", "New City", "9876543210");
        Owner existingOwner = new Owner(); // Giả lập owner lấy từ DB
        // existingOwner.setId(ownerId);
        existingOwner.setFirstName("George"); // Dữ liệu cũ

        given(ownerRepository.findById(ownerId)).willReturn(Optional.of(existingOwner));
        // Giả lập mapper sẽ cập nhật existingOwner
        given(ownerEntityMapper.map(eq(existingOwner), eq(request))).willReturn(existingOwner); // Trả về chính instance đã cập nhật
        // Giả lập save không trả về gì (do update) nhưng vẫn thành công
        given(ownerRepository.save(any(Owner.class))).willReturn(existingOwner); // Hoặc không cần mock save nếu nó không ảnh hưởng kết quả test

        mvc.perform(put("/owners/{ownerId}", ownerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(ownerRepository).findById(ownerId);
        verify(ownerEntityMapper).map(eq(existingOwner), eq(request));
        verify(ownerRepository).save(existingOwner); // Verify lưu đối tượng đã được map
    }

    @Test
    void testUpdateOwnerNotFound() throws Exception {
        int ownerId = 99;
        OwnerRequest request = new OwnerRequest("Update", "NotFound", "Addr", "City", "123");
        given(ownerRepository.findById(ownerId)).willReturn(Optional.empty());

        mvc.perform(put("/owners/{ownerId}", ownerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound()); // Do controller có orElseThrow
    }

     @Test
    void testUpdateOwnerBadRequestInvalidId() throws Exception {
         OwnerRequest request = new OwnerRequest("Update", "NotFound", "Addr", "City", "123");
         mvc.perform(put("/owners/{ownerId}", 0) // ID không hợp lệ
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

}