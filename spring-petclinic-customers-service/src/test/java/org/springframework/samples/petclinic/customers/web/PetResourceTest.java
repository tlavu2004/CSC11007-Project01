package org.springframework.samples.petclinic.customers.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.customers.model.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

// --- Thêm các import static cần thiết ---
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat; // Import argThat
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(PetResource.class)
@ActiveProfiles("test")
class PetResourceTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    // --- @MockBean vẫn dùng bình thường với @WebMvcTest ---
    @MockBean
    PetRepository petRepository;

    @MockBean
    OwnerRepository ownerRepository;

    private Owner owner;
    private Pet pet1;
    private Pet pet2;
    private PetType dogType;
    private PetType catType;

    @BeforeEach
    void setUp() {
        owner = new Owner();
        // Giả lập ID cho Owner nếu cần test equals/hashCode hoặc các logic phụ thuộc ID
        // owner.setId(1);
        owner.setFirstName("Test");
        owner.setLastName("Owner");

        dogType = new PetType();
        dogType.setId(1); // Gán ID cho PetType
        dogType.setName("Dog");

        catType = new PetType();
        catType.setId(2); // Gán ID cho PetType
        catType.setName("Cat");

        pet1 = new Pet();
        pet1.setId(1); // Gán ID cho Pet
        pet1.setName("Buddy");
        pet1.setType(dogType);
        pet1.setBirthDate(Date.from(Instant.parse("2020-01-01T00:00:00Z")));
        pet1.setOwner(owner);

        pet2 = new Pet();
        pet2.setId(2); // Gán ID cho Pet
        pet2.setName("Lucy");
        pet2.setType(catType);
        pet2.setBirthDate(Date.from(Instant.parse("2021-05-10T00:00:00Z")));
        pet2.setOwner(owner);
    }


    @Test
    void shouldGetAPetInJSonFormat() throws Exception {
        // Arrange
        given(petRepository.findById(1)).willReturn(Optional.of(pet1)); // Dùng willReturn

        // Act & Assert
        mvc.perform(get("/owners/1/pets/{petId}", 1).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Buddy"))
            .andExpect(jsonPath("$.owner").value("Test Owner"))
            .andExpect(jsonPath("$.type.id").value(1)) // ID của dogType
            .andExpect(jsonPath("$.type.name").value("Dog"));
    }

    @Test
    void testGetPetNotFound() throws Exception {
        // Arrange
        given(petRepository.findById(99)).willReturn(Optional.empty()); // Dùng willReturn

        // Act & Assert
        mvc.perform(get("/owners/1/pets/{petId}", 99).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }


    @Test
    void testGetPetTypesSuccess() throws Exception {
        // Arrange
        List<PetType> petTypes = Arrays.asList(dogType, catType);
        given(petRepository.findPetTypes()).willReturn(petTypes); // Dùng willReturn

        // Act & Assert
        mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2))) // Sử dụng hasSize đã import
            .andExpect(jsonPath("$[0].name").value("Dog"))
            .andExpect(jsonPath("$[1].name").value("Cat"));
    }

    @Test
    void testCreatePetSuccess() throws Exception {
        // Arrange
        int ownerId = 1; // Giả sử ownerId hợp lệ
        PetRequest request = new PetRequest(0, Date.from(Instant.parse("2023-01-15T00:00:00Z")), "NewPet", dogType.getId());

        Pet createdPet = new Pet();
        createdPet.setId(3);
        createdPet.setName(request.name());
        createdPet.setBirthDate(request.birthDate());
        createdPet.setType(dogType);
        createdPet.setOwner(owner);

        given(ownerRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petRepository.findPetTypeById(request.typeId())).willReturn(Optional.of(dogType));
        given(petRepository.save(any(Pet.class))).willReturn(createdPet); // Dùng willReturn

        // Act & Assert
        mvc.perform(post("/owners/{ownerId}/pets", ownerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.name").value("NewPet"))
            .andExpect(jsonPath("$.type.id").value(dogType.getId()));

        verify(petRepository).save(any(Pet.class));
    }

    @Test
    void testCreatePetOwnerNotFound() throws Exception {
        // Arrange
        int invalidOwnerId = 99;
        PetRequest request = new PetRequest(0, new Date(), "NewPet", dogType.getId());
        given(ownerRepository.findById(invalidOwnerId)).willReturn(Optional.empty());

        // Act & Assert
        mvc.perform(post("/owners/{ownerId}/pets", invalidOwnerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void testUpdatePetSuccess() throws Exception {
        // Arrange
        int petIdToUpdate = 1;
        PetRequest request = new PetRequest(petIdToUpdate, Date.from(Instant.parse("2020-02-20T00:00:00Z")), "UpdatedBuddy", catType.getId());

        Pet updatedPet = new Pet(); // Giả lập trạng thái sau khi save
        updatedPet.setId(petIdToUpdate);
        updatedPet.setName(request.name());
        updatedPet.setBirthDate(request.birthDate());
        updatedPet.setType(catType);
        updatedPet.setOwner(owner);

        given(petRepository.findById(petIdToUpdate)).willReturn(Optional.of(pet1)); // Tìm thấy pet gốc
        given(petRepository.findPetTypeById(request.typeId())).willReturn(Optional.of(catType));
        given(petRepository.save(any(Pet.class))).willReturn(updatedPet); // Giả lập hàm save

        // Act & Assert
        mvc.perform(put("/owners/*/pets/{petId}", petIdToUpdate)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        // Verify rằng pet đã được cập nhật đúng và gọi save
        verify(petRepository).save(argThat(savedPet -> // Dùng argThat đã import
            savedPet.getId().equals(petIdToUpdate) &&
            savedPet.getName().equals(request.name()) &&
            savedPet.getType().getId().equals(request.typeId()) &&
            savedPet.getBirthDate().equals(request.birthDate()) // Kiểm tra cả ngày sinh
        ));
    }


    @Test
    void testUpdatePetNotFound() throws Exception {
        // Arrange
        int invalidPetId = 99;
        PetRequest request = new PetRequest(invalidPetId, new Date(), "UpdateNotFound", dogType.getId());
        given(petRepository.findById(invalidPetId)).willReturn(Optional.empty());

        // Act & Assert
        mvc.perform(put("/owners/*/pets/{petId}", invalidPetId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

         verify(petRepository, never()).save(any(Pet.class));
    }
}