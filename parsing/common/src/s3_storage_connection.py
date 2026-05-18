from contextlib import asynccontextmanager
from aiobotocore.session import get_session
from botocore.config import Config
import asyncio
import os

# from dotenv import load_dotenv
# load_dotenv("common/s3_settings.env")

S3_ACCESS_KEY = os.getenv("S3_ACCESS_KEY")
S3_SECRET_KEY = os.getenv("S3_SECRET_KEY")
S3_ENDPOINT_URL = os.getenv("S3_ENDPOINT_URL")
S3_ADDRESSING_STYLE = os.getenv("S3_ADDRESSING_STYLE")
S3_REGION_NAME = os.getenv("S3_REGION_NAME")

S3_TIME_TO_LIVE_FLAG = True if os.getenv("S3_TIME_TO_LIVE_FLAG") == "True" else False
DEFAULT_BUCKET_NAME = os.getenv("DEFAULT_BUCKET_NAME")
CUSTOM_TTL_BUCKET_NAME = os.getenv("CUSTOM_TTL_BUCKET_NAME")


class S3Storage:
    def __init__(
            self, 
            access_key : str = S3_ACCESS_KEY,
            secret_key : str = S3_SECRET_KEY,
            endpoint_url : str = S3_ENDPOINT_URL,
            addressing_style : str = S3_ADDRESSING_STYLE,
            region_name : str = S3_REGION_NAME,
            withTimeToLive : bool = S3_TIME_TO_LIVE_FLAG
    ):
        self.config = {
            "aws_access_key_id": access_key,
            "aws_secret_access_key": secret_key,
            "endpoint_url": endpoint_url,
            "region_name": region_name,
            "config": Config(s3={"addressing_style": addressing_style})
        }
        self.session = get_session()
        self.bucket_name = CUSTOM_TTL_BUCKET_NAME if withTimeToLive else DEFAULT_BUCKET_NAME

    @asynccontextmanager
    async def get_client(self):
        async with self.session.create_client("s3", **self.config) as client:
            yield client
    
    async def file_exists(self, s3_object_key) -> bool:
        try:
            async with self.get_client() as client:
                await client.head_object(Bucket=self.bucket_name, Key=s3_object_key)
            return True
        except Exception as e:
            return False
        
    async def upload_file_bytes(self, s3_object_key, file_bytes):
        async with self.get_client() as client:
            await client.put_object(Bucket=self.bucket_name, Key=s3_object_key, Body=file_bytes)
    
    async def download_file_bytes(self, s3_object_key) -> bytes:
        async with self.get_client() as client:
            response = await client.get_object(Bucket=self.bucket_name, Key=s3_object_key)
            async with response["Body"] as stream:
                return await stream.read()
    
    async def get_object_keys_by_prefix(self, prefix):
        continuation_token = None
        s3_object_keys = []

        async with self.get_client() as client:
            while True:
                params = {
                    "Bucket": self.bucket_name,
                    "Prefix": prefix,
                }
                if continuation_token:
                    params["ContinuationToken"] = continuation_token

                response = await client.list_objects_v2(**params)
                contents = response.get("Contents", [])
                s3_object_keys += [obj["Key"] for obj in contents]

                if not response.get("IsTruncated"):
                    break
                continuation_token = response.get("NextContinuationToken")

        return s3_object_keys

    async def delete_file(self, s3_object_key):
        async with self.get_client() as client:
            await client.delete_object(Bucket=self.bucket_name, Key=s3_object_key)



