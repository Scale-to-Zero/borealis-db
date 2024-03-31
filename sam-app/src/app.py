import datetime
import json
import sqlite3
import traceback

def lambda_handler(event, context, DB_FILE="/mnt/lambda/sqlite.db"):
    conn = sqlite3.connect(DB_FILE)

    try:
        cursor = conn.cursor()

        # Extract the SQL query from the event
        query = event.get("query")
        started = datetime.datetime.now().timestamp()

        output = {}

        try:
            # Execute the SQL query
            cursor.execute(query)

            # Get the number of rows affected by the query
            output['rowsAffected'] = cursor.rowcount

            # Check if the query was a SELECT
            if cursor.description:
                # Fetch all the rows returned by the query
                rows = cursor.fetchall()

                output['column_names'] = [description[0] for description in cursor.description]
                output['rows'] = rows
            else:
                # For data modification statements
                output['rows'] = []
                output['column_names'] = []
                conn.commit()

            elapsed = round(datetime.datetime.now().timestamp() - started, 3)
            print(json.dumps({"output": output, "elapsed": elapsed, "query": query}))

            return {
                "statusCode": 200,
                "body": json.dumps(output)
            }

        except Exception as e:
            trace = traceback.format_exc()
            print(f"Error executing query: {e}\nTraceback:\n{trace}")
            return {
                "statusCode": 500,
                "body": json.dumps({"error": str(e)}),
            }

    finally:
        # Close the database connection
        conn.close()