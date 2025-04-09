package org.springframework.samples.petclinic.customers.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OwnerTest {

    private Owner owner;
    private Pet pet1;
    private Pet pet2;

    @BeforeEach
    void setUp() {
        owner = new Owner();
        owner.setFirstName("Test");
        owner.setLastName("Owner");

        PetType dogType = new PetType();
        dogType.setId(1);
        dogType.setName("Dog");

        pet1 = new Pet();
        pet1.setId(1); // Gán ID giả lập cho pet1
        pet1.setName("Buddy");
        pet1.setType(dogType);
        // Không set owner ở đây, để addPet xử lý

        pet2 = new Pet();
        pet2.setId(2); // Gán ID giả lập cho pet2
        pet2.setName("Lucy");
        pet2.setType(dogType);
         // Không set owner ở đây
    }

    @Test
    void testAddPet() {
        Set<Pet> internalPets = owner.getPetsInternal();
        int initialSize = internalPets.size();

        // Act
        owner.addPet(pet1);

        // Assert
        Set<Pet> updatedInternalPets = owner.getPetsInternal();
        assertEquals(initialSize + 1, updatedInternalPets.size(), "Pet set size should increase by 1");
        assertSame(owner, pet1.getOwner(), "Owner should be set on the pet");

        boolean foundPet1 = updatedInternalPets.stream()
                                            .anyMatch(pet -> pet.getId().equals(pet1.getId()) &&
                                                         pet.getName().equals(pet1.getName()));
        assertTrue(foundPet1, "Internal pet set should contain the added pet (checked by properties)");

    }

     @Test
    void testGetPetsWhenNoPets() {
         assertTrue(owner.getPets().isEmpty());
    }

    @Test
    void testGetPetsReturnsSortedAndUnmodifiableList() {
        // Thêm pet không theo thứ tự tên
        owner.addPet(pet2); // Lucy
        owner.addPet(pet1); // Buddy

        List<Pet> pets = owner.getPets();

        assertEquals(2, pets.size());
        assertEquals("Buddy", pets.get(0).getName()); // Kiểm tra đã sắp xếp
        assertEquals("Lucy", pets.get(1).getName());

        // Kiểm tra tính không thể thay đổi (unmodifiable)
        assertThrows(UnsupportedOperationException.class, () -> pets.add(new Pet()));
        assertThrows(UnsupportedOperationException.class, () -> pets.remove(0));
    }

     @Test
    void testGetPetsInternalInitializesSet() {
        assertNotNull(owner.getPetsInternal());
    }

    @Test
    void testEqualsAndHashCode() {
        Owner owner1 = new Owner();
        owner1.setFirstName("John");
        owner1.setLastName("Doe");

        Owner owner2 = new Owner();
        owner2.setFirstName("John");
        owner2.setLastName("Doe");

        Owner owner3 = new Owner();
        owner3.setFirstName("Jane");
        owner3.setLastName("Doe");
    }
}