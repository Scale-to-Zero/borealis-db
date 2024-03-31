# Usage:
# aws configure
# docker build -t borealis-builder .
# docker run --rm -v ~/.aws:/root/.aws -it borealis-builder

FROM public.ecr.aws/sam/build-python3.12

ENV AWS_PROFILE=dev
ENV AWS_DEFAULT_REGION=us-west-2
ENV SAM_STACK_NAME=borealis-demo

WORKDIR /sam-app
COPY sam-app /sam-app
RUN python tests/unit/test_handler.py

CMD ["/bin/bash", "-c", "sam build && sam deploy --stack-name $SAM_STACK_NAME"]
