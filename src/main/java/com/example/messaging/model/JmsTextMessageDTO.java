package com.example.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class JmsTextMessageDTO {

	private int id;
	private String content;
}