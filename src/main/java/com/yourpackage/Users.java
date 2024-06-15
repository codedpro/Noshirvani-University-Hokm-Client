package com.yourpackage;

import java.io.*;
import java.util.Objects;

public class Users
{
    public String registerUser(String username, String password)
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt", true)))
        {
            if (Objects.equals(isUsernameTaken(username), "Yes"))
            {
                return "This username is taken!";
            }
            writer.write(username + "," + password + "\n");
            return "Registration successful";
        }
        catch (IOException e)
        {
            return "An error occurred.";
        }
    }

    public String isUsernameTaken(String username)
    {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt")))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                String[] parts = line.split(",");
                if (parts[0].equals(username))
                {
                    return "Yes";
                }
            }
        }
        catch (IOException e)
        {
            return "An error occurred.";
        }
        return "No";
    }

    public String loginUser(String username , String password)
    {
        try(BufferedReader reader = new BufferedReader(new FileReader("users.txt")))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                String[] parts = line.split(",");
                if (Objects.equals(parts[0], username) && Objects.equals(parts[1], password))
                {
                    return "Login successful.";
                }
            }
        }
        catch (IOException e)
        {

            return "An error occurred.";
        }
        return "username or password is incorrect!";
    }
}
