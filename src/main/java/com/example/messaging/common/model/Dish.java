package com.example.messaging.common.model;

import lombok.*;

@Builder(setterPrefix = "with")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Dish {

	private Long id;

	private boolean cooked;

	private String name;
}
