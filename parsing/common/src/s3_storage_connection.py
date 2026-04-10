from contextlib import asynccontextmanager
from aiobotocore.session import get_session
from botocore.exceptions import ClientError
import asyncio

DEFAULT_BUCKET_NAME = "garage-default-bucket"
TTL_7D_BUCKET_NAME = "garage-ttl-7d-bucket"

class S3Storage:
    def __init__(
            self, 
            access_key : str,
            secret_key : str,
            endpoint_url : str,
            withTimeToLive : bool
    ):
        self.config = {
            "aws_access_key_id": access_key,
            "aws_secret_access_key": secret_key,
            "endpoint_url": endpoint_url,
            "region_name": "garage"
        }
        self.session = get_session()
        self.bucket_name = TTL_7D_BUCKET_NAME if withTimeToLive else DEFAULT_BUCKET_NAME

    @asynccontextmanager
    async def get_client(self):
        async with self.session.create_client("s3", **self.config) as client:
            yield client
        
    async def delete_file(self, s3_object_key):
        async with self.get_client() as client:
            await client.delete_object(Bucket=self.bucket_name, Key=s3_object_key)

    async def upload_file_bytes(self, file_bytes, s3_object_key):
        async with self.get_client() as client:
            await client.put_object(Bucket=self.bucket_name, Key=s3_object_key, Body=file_bytes)
    
    async def download_file_bytes(self, s3_object_key) -> bytes:
        async with self.get_client() as client:
            response = await client.get_object(Bucket=self.bucket_name, Key=s3_object_key)
            async with response["Body"] as stream:
                return await stream.read()
            
    async def file_exists(self, s3_object_key) -> bool:
        try:
            async with self.get_client() as client:
                await client.head_object(Bucket=self.bucket_name, Key=s3_object_key)
            return True
        except Exception as e:
            return False


async def main():
    s3_client = S3Storage(
        access_key="GKb1b6b3a6cae1445a5a17a087",
        secret_key="549eff9a670f17cd878edb8f5ffa170f0f1935f96dcc2c501928730f47850f2c",
        endpoint_url="http://localhost:3900",
        withTimeToLive=False
    )
    print(await s3_client.file_exists("cat.jpeg"))
    print(await s3_client.download_file_bytes("cat.jpeg"))
    # await s3_client.upload_file("cat.jpeg")

if __name__ == "__main__":
    asyncio.run(main())

