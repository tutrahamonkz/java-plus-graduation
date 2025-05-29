package ru.practicum.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDto {

    @Email()
    @NotBlank
    @Size(min = 6, max = 254)
    private String email;

    private Long id;

    @NotBlank
    @Size(min = 2, max = 250)
    private String name;
}
