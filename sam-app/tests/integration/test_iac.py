import os
import unittest
import boto3
import json
import subprocess

stack_name = 'borealis-integration-test'
template_file = 'template.yaml'

assert os.path.exists(template_file), f"Cannot find template file {template_file} in {os.getcwd()}"

print(f"Using AWS profile {os.environ.get('AWS_PROFILE', 'default')}")

sts_client = boto3.client('sts')
response = sts_client.get_caller_identity()
print(f"Running tests with AWS account {response['Account']} and IAM Role {response['Arn']}")

class TestSQS(unittest.TestCase):
    function_name: str

    @classmethod
    def setUpClass(cls):
        # Deploy or update the stack using SAM
        cls.deploy_or_update_stack()

    @classmethod
    def deploy_or_update_stack(cls):
        command = [
            'sam', 'deploy',
            '--template-file', template_file,
            '--stack-name', stack_name,
            '--capabilities', 'CAPABILITY_IAM',
            '--no-fail-on-empty-changeset'
        ]

        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)

        with process.stdout, process.stderr:
            while True:
                output = process.stdout.readline()
                if output == '' and process.poll() is not None:
                    break
                if output:
                    print(output.strip())

            rc = process.poll()
            if rc != 0:
                stderr = process.stderr.read()
                raise Exception(f"Failed to deploy or update stack {stack_name}:\n{stderr}")
            else:
                print(f"Stack {stack_name} deployed or updated successfully")

    def setUp(self) -> None:
        client = boto3.client("cloudformation")

        response = client.describe_stacks(StackName=stack_name)
        stacks = response["Stacks"]
        self.assertTrue(stacks, f"Cannot find stack {stack_name}")

        self.function_name = next(
            output["OutputValue"] for output in stacks[0]["Outputs"] if output["OutputKey"] == "DbEngineFunction"
        )

        print(f"Function name: {self.function_name}")

        self.assertTrue(self.function_name, "Function name not found")

    def test_lambda_function(self):
        """
        Invoke the Lambda function with a query, verify the response contains the expected keys.
        """
        lambda_client = boto3.client("lambda")

        query = "SELECT 2+1;"
        response = lambda_client.invoke(
            FunctionName=self.function_name,
            Payload=json.dumps({"query": query}),
        )
        print(f"Response: {response}")

        payload = json.loads(response["Payload"].read())
        print(f"Payload: {payload}")

        self.assertEqual(payload["statusCode"], 200, "Unexpected status code")
        result = json.loads(payload["body"])["result"]

        self.assertIsInstance(result, list, "Result is not a list")
        self.assertTrue(result, "Result is empty")

        first_row = result[0]
        self.assertIn("2+1", first_row, "Key '2+1' not found in the result")

if __name__ == "__main__":
    unittest.main()