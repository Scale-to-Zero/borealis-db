import json
import unittest
import os
from unittest.mock import patch
import sys
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..'))
from src.app import lambda_handler

class TestLambdaHandler(unittest.TestCase):
    def test_lambda_handler(self):
        create_table_query = "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name TEXT)"
        insert_queries = [
            "INSERT INTO users (id, name) VALUES (1, 'John Doe')",
            "INSERT INTO users (id, name) VALUES (2, 'Jane Smith')"
        ]
        select_query = "SELECT * FROM users"

        # Create the table and insert data
        for query in [create_table_query] + insert_queries:
            event = {"query": query}
            lambda_handler(event, "", DB_FILE='test.db')

        # Select the data
        event = {"query": select_query}
        result = lambda_handler(event, "", DB_FILE='test.db')

        expected_result = {
            "statusCode": 200,
            "body": json.dumps({
                "rowsAffected": -1,
                "column_names": ["id", "name"],
                "rows": [[1, "John Doe"], [2, "Jane Smith"]],
            }),
        }
        self.assertEqual(result, expected_result)

if __name__ == "__main__":
    unittest.main()