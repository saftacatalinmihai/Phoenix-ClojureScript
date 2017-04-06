defmodule Backend.CodeServer do
  @moduledoc false
  
  use GenServer

  def start_link do
    GenServer.start_link(__MODULE__, :ok, name: CodeServer)
  end

  def get_actor_types do
    GenServer.call(CodeServer, :get_actor_types)
  end
  
  def new_actor_type(actor_type) do
    GenServer.call(CodeServer, {:new_actor_type, actor_type})
  end

  def init(:ok) do
    actor_types = getActors() |> evalActors
    {:ok, %{:actor_types => actor_types}}
  end

  def handle_call(:get_actor_types, _from, state) do
    {:reply, state[:actor_types], state}
  end

  def handle_call({:new_actor_type, actor_type}, _from, %{:actor_types => actor_types}) do
    case File.read("code/template/actor.ex") do
      {:ok, body} ->
        {:ok, file} = File.open "code/#{actor_type}.ex", [:write]
        IO.binwrite file, String.replace(body, "{{actor_name}}", actor_type)
        Code.eval_file("code/#{actor_type}.ex")
        {:reply, {:ok, %{:actor_type => actor_type}}, %{:actor_types => [actor_type | actor_types]}}
      {:error, reason} ->
        IO.inspect reason
        {:reply, {:error, %{:reason => reason}}, %{:actor_types => actor_types}}
    end
  end

  def handle_call(_msg, _from, state) do
    {:reply, :ok, state}
  end

  def handle_cast(_msg, state) do
    {:noreply, state}
  end

  defp getActors do
    Path.wildcard("code/*.ex") |>
    Enum.map(fn file ->
       String.replace_prefix(file, "code/", "") |>
       String.replace_suffix(".ex", "")
    end)
  end

  defp evalActors(actors) do
    actors |> Enum.map(&evalActor/1)
  end

  defp evalActor(type) do
    Code.eval_file("code/#{type}.ex")
    type
  end

end
