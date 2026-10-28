package com.example.readerai.dto;

import com.example.readerai.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {

    private String name;

    private UserRole role;
}
