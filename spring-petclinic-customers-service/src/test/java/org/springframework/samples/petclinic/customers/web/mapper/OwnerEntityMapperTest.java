package org.springframework.samples.petclinic.customers.web.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.web.OwnerRequest;

import static org.junit.jupiter.api.Assertions.*;

class OwnerEntityMapperTest {

    private final OwnerEntityMapper mapper = new OwnerEntityMapper();

    @Test
    void shouldMapOwnerRequestToOwnerEntity() {
        // Arrange
        OwnerRequest request = new OwnerRequest(
            "TestFirstName",
            "TestLastName",
            "123 Test Address",
            "TestCity",
            "1234567890"
        );
        Owner owner = new Owner();

        // Act
        Owner mappedOwner = mapper.map(owner, request);

        // Assert
        assertNotNull(mappedOwner);
        assertEquals("TestFirstName", mappedOwner.getFirstName());
        assertEquals("TestLastName", mappedOwner.getLastName());
        assertEquals("123 Test Address", mappedOwner.getAddress());
        assertEquals("TestCity", mappedOwner.getCity());
        assertEquals("1234567890", mappedOwner.getTelephone());
        // Đảm bảo instance gốc được cập nhật
        assertSame(owner, mappedOwner);
    }
}