package org.springframework.samples.petclinic.customers.model;

import org.junit.jupiter.api.Test;
import java.util.Date;

// --- Import static cho Hamcrest ---
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;


class PetTest {

     @Test
    void testEqualsAndHashCode() {
        PetType catType = new PetType();
        catType.setId(1);
        catType.setName("Cat");

        Owner owner = new Owner();
        owner.setFirstName("Test");
        owner.setLastName("Owner");


        Pet pet1 = new Pet();
        pet1.setId(1);
        pet1.setName("Leo");
        pet1.setBirthDate(new Date());
        pet1.setType(catType);
        pet1.setOwner(owner);

        Pet pet2 = new Pet();
        pet2.setId(1);
        pet2.setName("Leo");
        pet2.setBirthDate(pet1.getBirthDate()); // Cùng ngày sinh
        pet2.setType(catType);
        pet2.setOwner(owner);

        Pet pet3 = new Pet();
        pet3.setId(2); // ID khác
        pet3.setName("Leo");
        pet3.setBirthDate(pet1.getBirthDate());
        pet3.setType(catType);
        pet3.setOwner(owner);

        Pet pet4 = new Pet();
        pet4.setId(1);
        pet4.setName("Lily"); // Tên khác
        pet4.setBirthDate(pet1.getBirthDate());
        pet4.setType(catType);
        pet4.setOwner(owner);

        assertEquals(pet1, pet2, "Pets with same properties should be equal");
        assertEquals(pet1.hashCode(), pet2.hashCode(), "Hashcode should be same for equal objects");

        assertNotEquals(pet1, pet3, "Pets with different IDs should not be equal");

        assertNotEquals(pet1, pet4, "Pets with different names should not be equal");

        assertNotEquals(pet1, null, "Pet should not be equal to null");
        assertNotEquals(pet1, new Object(), "Pet should not be equal to a different type object");
    }


     @Test
    void testToString() {
        // Arrange
        PetType catType = new PetType();
        catType.setId(1);
        catType.setName("Cat"); // Name của PetType

        Owner owner = new Owner();
        owner.setFirstName("TestFirst"); // FirstName của Owner
        owner.setLastName("TestLast"); // LastName của Owner

        Pet pet = new Pet();
        pet.setId(10);
        pet.setName("Whiskers"); // Name của Pet
        pet.setBirthDate(new Date(0)); // Ngày sinh để kiểm tra format nếu cần
        pet.setType(catType);
        pet.setOwner(owner);

        assertEquals("Whiskers", pet.getName(), "Pre-check: getName() should return correct name");

        // Act
        String toStringResult = pet.toString();
        System.out.println("DEBUG - Actual toString() output for Pet: [" + toStringResult + "]");

        assertThat("toString output", toStringResult, containsString("id = 10"));
        assertThat("toString output", toStringResult, containsString("name = 'Whiskers'"));
        
        assertThat("toString output", toStringResult, containsString("type = 'Cat'"));
        assertThat("toString output", toStringResult, containsString("ownerFirstname = 'TestFirst'"));
        assertThat("toString output", toStringResult, containsString("ownerLastname = 'TestLast'"));
    }
}