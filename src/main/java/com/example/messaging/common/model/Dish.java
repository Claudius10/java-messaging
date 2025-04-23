package com.example.messaging.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder(setterPrefix = "with")
@AllArgsConstructor
@Getter
@Setter
public class Dish {

	private Long id;

	private boolean cooked;

	private String name;
}
