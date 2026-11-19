package com.collegequiz.redesign.model;

public record User(int id, String fullName, String email, String role) {
    public boolean isTeacher() { return "TEACHER".equals(role); }
}